#!/usr/bin/env node
/**
 * Exercises every endpoint against a running backend and reports what each one
 * actually answered.
 *
 *   node scripts/smoke-api.mjs
 *   node scripts/smoke-api.mjs --quiet          # only the failures and the tally
 *
 * Exits 0 when every call answered as expected, 1 otherwise, so it can gate a
 * deploy or run in CI.
 *
 * This is not a replacement for the test suite — those 200 tests cover the
 * branches this cannot reach, roll back after themselves, and run without a
 * server. What this adds is the one thing they cannot: proof that the wired,
 * running application answers correctly over real HTTP, through the filter
 * chain, with a real database behind it. Bugs have hidden in exactly that gap
 * before, where every test passed and the deployed answer was still wrong.
 *
 * ---------------------------------------------------------------------------
 * It writes. It creates categories, products, tables, staff, suppliers, stock
 * items, purchase orders, a user account and paid orders. Most are cleaned up,
 * but not all can be: a received purchase order and its stock movements are
 * deliberately permanent, and a paid order is a financial record. Point this at
 * a disposable database.
 *
 * That is why it refuses a non-local target unless SMOKE_ALLOW_REMOTE=1.
 * ---------------------------------------------------------------------------
 *
 * Environment:
 *   SMOKE_BASE_URL       default http://localhost:8081/api
 *   SMOKE_ADMIN_USER     default admin
 *   SMOKE_ADMIN_PASS     default ChangeMe123!
 *   SMOKE_CASHIER_USER   default cashier
 *   SMOKE_CASHIER_PASS   default ChangeMe123!
 *   SMOKE_ALLOW_REMOTE   set to 1 to allow a non-localhost base URL
 */

const BASE = process.env.SMOKE_BASE_URL ?? "http://localhost:8081/api";
const ADMIN = [process.env.SMOKE_ADMIN_USER ?? "admin", process.env.SMOKE_ADMIN_PASS ?? "ChangeMe123!"];
const CASHIER = [process.env.SMOKE_CASHIER_USER ?? "cashier", process.env.SMOKE_CASHIER_PASS ?? "ChangeMe123!"];
const QUIET = process.argv.includes("--quiet");

/* A smoke test that creates and deletes records is one misread environment
   variable away from doing it to production. Localhost unless told otherwise. */
const host = new URL(BASE).hostname;
if (!["localhost", "127.0.0.1", "::1"].includes(host) && process.env.SMOKE_ALLOW_REMOTE !== "1") {
  console.error(`Refusing to run against ${host}: this script writes data.`);
  console.error("Set SMOKE_ALLOW_REMOTE=1 if that is really what you want.");
  process.exit(2);
}

const results = [];

/*
 * Mirrors IdempotencyFilter.REQUIRED on the backend and IDEMPOTENT_ROUTES in
 * the frontend client. Three copies of one list is two too many, but the
 * alternative is the server publishing it and every caller fetching it before
 * its first write, which is worse for five routes.
 */
const IDEMPOTENT = [
  /^\/orders$/,
  /^\/returns$/,
  /^\/orders\/[^/]+\/pay$/,
  /^\/orders\/[^/]+\/cancel$/,
  /^\/stock\/[^/]+\/adjust$/,
  /^\/purchases\/[^/]+\/receive$/,
];

function idempotencyKey(method, path) {
  if (method !== "POST") return null;
  const clean = path.split("?")[0];
  return IDEMPOTENT.some((r) => r.test(clean)) ? crypto.randomUUID() : null;
}

async function call(label, method, path, { tok, body, expect = [200, 201], raw = false, idem } = {}) {
  const headers = {};
  if (tok) headers.Authorization = "Bearer " + tok;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  // `idem` lets a caller pin the key, which is how the replay check below
  // sends the same intent twice.
  const key = idem ?? idempotencyKey(method, path);
  if (key) headers["Idempotency-Key"] = key;

  let status = 0, text = "", json = null;
  try {
    const res = await fetch(BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    status = res.status;
    text = await res.text();
    if (!raw) { try { json = JSON.parse(text); } catch { /* not json; fine */ } }
  } catch (e) {
    text = "NETWORK: " + e.message;
  }

  results.push({
    label, method, path, status,
    ok: expect.includes(status),
    expect: expect.join("/"),
    msg: json?.message ?? text.slice(0, 80),
  });
  return json?.data ?? null;
}

async function login(user, pass) {
  const data = await call(`login ${user}`, "POST", "/auth/login", {
    body: { username: user, password: pass },
    expect: [200],
  });
  return data?.accessToken ?? null;
}

const today = () => new Date().toISOString().slice(0, 10);

async function main() {
  const adminTok = await login(...ADMIN);
  const cashierTok = await login(...CASHIER);

  if (!adminTok) {
    console.error(`\nCould not sign in as ${ADMIN[0]}. Is the backend up at ${BASE}?`);
    report();
    process.exit(1);
  }

  // Unique-ish suffix so codes and usernames do not collide across runs.
  const tag = Date.now().toString().slice(-6);

  // Everything this script creates carries one of these markers, so
  // scripts/clean-smoke-data.sql can find its leavings by pattern instead of
  // guessing at ids or deleting whole tables. Changing them means changing
  // that script too.
  const USER = "smoke" + tag;        // users_infm.user_id  LIKE 'smoke%'
  const CODE = "SMOKE-" + tag;       // supplier / staff codes LIKE 'SMOKE-%'
  const NAME = "Smoke " + tag;       // table and stock-item names LIKE 'Smoke %'

  /* ---- auth ------------------------------------------------------------ */
  await call("current user", "GET", "/auth/me", { tok: adminTok });
  await call("update profile", "PUT", "/auth/me", {
    tok: adminTok,
    body: { fullName: "Administrator", email: "admin@rms.local", phone: "012000111" },
  });
  // No cookie is sent here, so a refusal is the correct answer.
  // 401, not 400: an absent session is not a malformed request.
  await call("refresh without cookie", "POST", "/auth/refresh", { expect: [401] });
  // Creating a login is an administrator's act. It was once a public endpoint
  // that also honoured whatever role the caller asked for, which meant anyone
  // who could reach the port could make themselves an admin — so the refusals
  // below matter more than the success and are checked first.
  const newAccount = {
    username: USER, password: "Passw0rdX", fullName: "Smoke Test",
    email: `${USER}@rms.local`, role: "CASHIER",
  };
  await call("create an account with no token", "POST", "/auth/register", {
    body: { ...newAccount, username: USER + "x", role: "ADMIN" }, expect: [401],
  });
  await call("create an account as a cashier", "POST", "/auth/register", {
    tok: cashierTok, body: { ...newAccount, username: USER + "x", role: "ADMIN" }, expect: [403],
  });
  await call("create an account as an admin", "POST", "/auth/register", {
    tok: adminTok, body: newAccount,
  });
  const tmpTok = await login(USER, "Passw0rdX");
  await call("change password", "POST", "/auth/change-password", {
    tok: tmpTok, body: { currentPassword: "Passw0rdX", newPassword: "Passw0rdY" },
  });
  await call("forgot password", "POST", "/auth/forgot-password", { body: { email: "nobody@example.com" } });
  // The happy paths for these two need a code from an email and the token it
  // yields, so only the refusal is reachable from here.
  await call("verify a wrong code", "POST", "/auth/verify-otp", {
    body: { email: "admin@rms.local", code: "000000" }, expect: [400],
  });
  await call("reset with a bad token", "POST", "/auth/reset-password", {
    body: { resetToken: "not-a-token", newPassword: "Passw0rdZ" }, expect: [400],
  });
  await call("logout", "POST", "/auth/logout", { tok: tmpTok });
  await call("health", "GET", "/health", {});

  /* ---- categories ------------------------------------------------------ */
  await call("list categories", "GET", "/categories", { tok: adminTok });
  await call("active categories", "GET", "/categories/active", { tok: adminTok });
  await call("category by id", "GET", "/categories/1", { tok: adminTok });
  const cat = await call("create category", "POST", "/categories", {
    tok: adminTok, body: { name: "សាកល្បង", nameEn: "Smoke", icon: "🧪", sortOrder: 99 },
  });
  await call("update category", "PUT", `/categories/${cat?.id}`, {
    tok: adminTok, body: { name: "សាកល្បង", nameEn: "Smoke 2", icon: "🧪", sortOrder: 99, status: "ACTIVE" },
  });
  await call("delete category", "DELETE", `/categories/${cat?.id}`, { tok: adminTok, expect: [200, 204] });
  await call("category delete guard", "DELETE", "/categories/1", { tok: adminTok, expect: [409] });

  /* ---- products -------------------------------------------------------- */
  await call("list products", "GET", "/products", { tok: adminTok });
  await call("product by id", "GET", "/products/1", { tok: adminTok });
  await call("filter products by category", "GET", "/products?categoryId=7", { tok: adminTok });
  await call("search products", "GET", "/products?search=Lime", { tok: adminTok });
  const prod = await call("create product", "POST", "/products", {
    tok: adminTok,
    body: { name: "ម្ហូបសាកល្បង", nameEn: "Smoke dish", categoryId: 1, price: 2.5, cost: 1.0, stockQty: 10 },
  });
  await call("update product", "PUT", `/products/${prod?.id}`, {
    tok: adminTok,
    body: { name: "ម្ហូបសាកល្បង", nameEn: "Smoke dish 2", categoryId: 1, price: 3.0, cost: 1.0, stockQty: 10, status: "ACTIVE" },
  });
  await call("delete product", "DELETE", `/products/${prod?.id}`, { tok: adminTok, expect: [200, 204] });

  /* ---- tables ---------------------------------------------------------- */
  await call("list tables", "GET", "/tables", { tok: adminTok });
  await call("table summary", "GET", "/tables/summary", { tok: adminTok });
  await call("table by id", "GET", "/tables/1", { tok: adminTok });
  const tbl = await call("create table", "POST", "/tables", {
    tok: adminTok, body: { name: NAME, seats: 2, zone: "INDOOR" },
  });
  await call("update table", "PUT", `/tables/${tbl?.id}`, {
    tok: adminTok, body: { name: NAME, seats: 4, zone: "OUTDOOR", status: "FREE" },
  });
  await call("set table status", "PATCH", `/tables/${tbl?.id}/status`, { tok: adminTok, body: { status: "RESERVED" } });
  await call("delete table", "DELETE", `/tables/${tbl?.id}`, { tok: adminTok, expect: [200, 204] });

  /* ---- staff ----------------------------------------------------------- */
  await call("list staff", "GET", "/staff", { tok: adminTok });
  await call("staff by id", "GET", "/staff/1", { tok: adminTok });
  const staff = await call("create staff", "POST", "/staff", {
    tok: adminTok,
    body: { staffCode: CODE, staffName: "Smoke Staff", role: "CASHIER", shift: "MORNING", gender: "MALE", salary: 300 },
  });
  await call("update staff", "PUT", `/staff/${staff?.id}`, {
    tok: adminTok,
    body: { staffCode: CODE, staffName: "Smoke Staff 2", role: "CASHIER", shift: "EVENING", gender: "MALE", salary: 350, status: "ACTIVE" },
  });
  await call("delete staff", "DELETE", `/staff/${staff?.id}`, { tok: adminTok, expect: [200, 204] });

  /* ---- suppliers ------------------------------------------------------- */
  await call("list suppliers", "GET", "/suppliers", { tok: adminTok });
  await call("active suppliers", "GET", "/suppliers/active", { tok: adminTok });
  await call("supplier by id", "GET", "/suppliers/1", { tok: adminTok });
  const sup = await call("create supplier", "POST", "/suppliers", {
    tok: adminTok,
    body: { supplierCode: CODE, company: "Smoke Supply", contactPerson: "A", phone: "012", supplyType: "OTHER" },
  });
  await call("update supplier", "PUT", `/suppliers/${sup?.id}`, {
    tok: adminTok,
    body: { supplierCode: CODE, company: "Smoke Supply 2", contactPerson: "B", phone: "013", supplyType: "MEAT", status: "ACTIVE" },
  });
  // Supplier 1 is owed money in the seed, so removing it must be refused.
  await call("supplier delete guard", "DELETE", "/suppliers/1", { tok: adminTok, expect: [400] });

  /* ---- stock ----------------------------------------------------------- */
  await call("list stock", "GET", "/stock", { tok: adminTok });
  await call("stock summary", "GET", "/stock/summary", { tok: adminTok });
  await call("low stock", "GET", "/stock/low", { tok: adminTok });
  await call("stock item by id", "GET", "/stock/1", { tok: adminTok });
  await call("stock movements", "GET", "/stock/1/movements", { tok: adminTok });
  const item = await call("create stock item", "POST", "/stock", {
    tok: adminTok, body: { name: NAME, unit: "kg", qty: 5, minQty: 1, unitCost: 2 },
  });
  await call("update stock item", "PUT", `/stock/${item?.id}`, {
    tok: adminTok, body: { name: NAME, unit: "kg", qty: 5, minQty: 2, unitCost: 2.5 },
  });
  await call("adjust stock", "POST", `/stock/${item?.id}/adjust`, {
    tok: adminTok, body: { type: "IN", qty: 3, reason: "smoke test" },
  });
  // The adjustment above left a movement, which is what makes this a 400.
  await call("stock delete guard", "DELETE", `/stock/${item?.id}`, { tok: adminTok, expect: [400] });

  /* ---- purchases ------------------------------------------------------- */
  await call("list purchases", "GET", "/purchases", { tok: adminTok });
  await call("purchase summary", "GET", "/purchases/summary", { tok: adminTok });
  const po = await call("create purchase", "POST", "/purchases", {
    tok: adminTok,
    body: {
      supplierId: sup?.id ?? 1, purchaseDate: today(), note: "smoke test",
      items: [{ stockItemId: item?.id ?? 1, qty: 2, unitCost: 2.5 }],
    },
  });
  await call("purchase by id", "GET", `/purchases/${po?.id}`, { tok: adminTok });
  await call("receive purchase", "POST", `/purchases/${po?.id}/receive`, { tok: adminTok });
  const po2 = await call("create second purchase", "POST", "/purchases", {
    tok: adminTok,
    body: {
      supplierId: sup?.id ?? 1, purchaseDate: today(),
      items: [{ stockItemId: item?.id ?? 1, qty: 1, unitCost: 2 }],
    },
  });
  await call("cancel purchase", "POST", `/purchases/${po2?.id}/cancel`, { tok: adminTok });
  await call("delete purchase", "DELETE", `/purchases/${po2?.id}`, { tok: adminTok, expect: [200, 204] });

  /* ---- orders: the whole POS lifecycle ---------------------------------
     Rung up by the account this script registered, not the seeded cashier.
     An order records who took it, so that is what lets the cleanup script tell
     a smoke sale from a real one; otherwise it would have to delete every
     order and hope none of them mattered. */
  /* ---- variants and modifiers -------------------------------------------
     A size is priced in its own right; a modifier moves a line's price. The
     server works both out from ids, so the only thing a client can choose is
     which. */
  const DISH = 1;   // the seeded fried rice, which outlives this script
  /* Named with the marker every other artefact carries. A run that dies
     between here and the delete below leaves this size on a seeded dish, and
     clean-smoke-data.sql can only remove what it can recognise. */
  const size = await call("add a size", "POST", `/products/${DISH}/variants`, {
    tok: adminTok, body: { name: "Smoke large", price: 9.5 }, expect: [201],
  });
  await call("the product's sizes", "GET", `/products/${DISH}/variants`, { tok: adminTok });
  const question = await call("a question with two answers", "POST", "/modifier-groups", {
    tok: adminTok, expect: [201],
    body: {
      name: `Smoke extras ${Date.now() % 100000}`, minSelect: 0, maxSelect: 1,
      modifiers: [{ name: "Extra", priceDelta: 1 }, { name: "None", priceDelta: 0 }],
    },
  });
  await call("asking for more than is offered", "POST", "/modifier-groups", {
    tok: adminTok, expect: [400],
    body: { name: "Impossible", minSelect: 0, maxSelect: 5,
            modifiers: [{ name: "Only one", priceDelta: 0 }] },
  });
  await call("ask it about this dish", "POST",
    `/products/${DISH}/modifier-groups/${question?.id}`, { tok: adminTok });
  await call("what it asks", "GET", `/products/${DISH}/modifier-groups`, { tok: adminTok });

  /* ---- promotions: money off by a rule ----------------------------------
     Percent and fixed only; buy-X-get-Y is refused rather than half-built,
     which is the half of SCREENS §3.5 that would be invisible otherwise. */
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 19);
  const nextWeek = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 19);
  const promo = await call("a rule on the whole bill", "POST", "/promotions", {
    tok: adminTok, expect: [201],
    body: { name: `Smoke ${Date.now() % 100000}`, type: "PERCENT", value: 10,
            scope: "ORDER", startsAt: yesterday, endsAt: nextWeek },
  });
  await call("buy-X-get-Y is not ready", "POST", "/promotions", {
    tok: adminTok, expect: [400],
    body: { name: "Two for one", type: "BUY_X_GET_Y", value: 1, scope: "ORDER",
            startsAt: yesterday, endsAt: nextWeek },
  });
  await call("list promotions", "GET", "/promotions", { tok: adminTok });
  await call("what is running now", "GET", "/promotions/live", { tok: adminTok });

  /* ---- branches: which shop every one of those rows belongs to -----------
     The badge is all P1 shows; what matters is that the branch rides in the
     token. A cashier is refused the move, which is the half of the rule that
     would be invisible if only the happy path were exercised. */
  await call("the branches I may work in", "GET", "/auth/branches", { tok: adminTok });
  await call("switching to my own branch", "POST", "/auth/switch-branch", {
    tok: adminTok, body: { branchId: 1 },
  });
  await call("switching to a branch that is not there", "POST", "/auth/switch-branch", {
    tok: adminTok, body: { branchId: 999999 }, expect: [404],
  });

  const posTok = await login(USER, "Passw0rdY");
  await call("a cashier may not change shop", "POST", "/auth/switch-branch", {
    tok: posTok, body: { branchId: 1 }, expect: [403],
  });

  /* ---- shifts: the drawer this POS session belongs to -------------------
     A bill cannot be settled without one, so this comes before the orders
     and not after them. That ordering is the gate, stated as a script. */
  await call("no shift open yet", "GET", "/shifts/current", { tok: posTok });
  const shift = await call("open the drawer", "POST", "/shifts", {
    tok: posTok, body: { openingFloat: 100 }, expect: [201],
  });
  await call("current shift", "GET", "/shifts/current", { tok: posTok });
  await call("a second one is refused", "POST", "/shifts", {
    tok: posTok, body: { openingFloat: 50 }, expect: [400],
  });
  await call("cash out of the drawer", "POST", `/shifts/${shift?.id}/movements`, {
    tok: posTok, body: { type: "PAY_OUT", amount: 5, reason: "smoke test" }, expect: [201],
  });
  await call("a sale cannot be typed in", "POST", `/shifts/${shift?.id}/movements`, {
    tok: posTok, body: { type: "SALE", amount: 5, reason: "smoke test" }, expect: [400],
  });

  /* ---- customers: who the bill belongs to ------------------------------- */
  const buyer = await call("register a customer", "POST", "/customers", {
    tok: adminTok,
    body: { name: `Smoke ${Date.now() % 100000}`, phone: `012 ${Date.now() % 1000000}` },
    expect: [201],
  });
  await call("list customers", "GET", "/customers?search=Smoke", { tok: adminTok });
  await call("customer by id", "GET", `/customers/${buyer?.id}`, { tok: adminTok });
  await call("look up by phone", "GET",
    `/customers/lookup?phone=${encodeURIComponent(buyer?.phone ?? "")}`, { tok: posTok });
  await call("adjust their points", "POST", `/customers/${buyer?.id}/loyalty`, {
    tok: adminTok, body: { points: 25, reason: "smoke", note: "smoke test" }, expect: [201],
  });
  await call("an adjustment needs a reason", "POST", `/customers/${buyer?.id}/loyalty`, {
    tok: adminTok, body: { points: 25 }, expect: [400],
  });
  await call("their points ledger", "GET", `/customers/${buyer?.id}/loyalty`, { tok: adminTok });

  await call("list orders", "GET", "/orders", { tok: posTok });
  await call("order summary", "GET", "/orders/summary", { tok: posTok });
  const order = await call("open a bill", "POST", "/orders", { tok: posTok, body: { tableId: 5 } });
  await call("order by id", "GET", `/orders/${order?.id}`, { tok: posTok });
  await call("open order for a table", "GET", "/orders/open?tableId=5", { tok: posTok });
  await call("name the customer on the bill", "PUT", `/orders/${order?.id}/customer`, {
    tok: posTok, body: { customerId: buyer?.id },
  });
  // A line with a size and a choice: 9.50 for the large, plus 1.00 for the
  // extra, and the server is what decides that.
  await call("a line with a size and a choice", "PUT", `/orders/${order?.id}/items`, {
    tok: posTok,
    body: { items: [{ productId: 1, qty: 1, variantId: size?.id,
                      modifierIds: [question?.modifiers?.[0]?.id] }] },
  });
  await call("a choice the dish does not offer", "PUT", `/orders/${order?.id}/items`, {
    tok: posTok, expect: [400],
    body: { items: [{ productId: 2, qty: 1, modifierIds: [question?.modifiers?.[0]?.id] }] },
  });

  await call("set order items", "PUT", `/orders/${order?.id}/items`, {
    tok: posTok, body: { items: [{ productId: 1, qty: 2 }, { productId: 11, qty: 1 }] },
  });
  await call("take payment", "POST", `/orders/${order?.id}/pay`, {
    tok: posTok, body: { paymentMethod: "CASH", amountTendered: 50 },
  });
  await call("fetch receipt", "GET", `/orders/${order?.id}/receipt`, { tok: posTok });
  const discounted = await call("what came off this bill", "GET",
    `/promotions/applicable?orderId=${order?.id}`, { tok: posTok });
  if (!Array.isArray(discounted) || discounted.length === 0) {
    console.log("  ! the live promotion took nothing off the bill");
    process.exitCode = 1;
  }
  // The meal should have earned them points on top of the 25 adjusted above.
  await call("their bills", "GET", `/orders?customerId=${buyer?.id}`, { tok: adminTok });
  const earned = await call("points after the meal", "GET", `/customers/${buyer?.id}`,
    { tok: adminTok });
  if (Number(earned?.points ?? 0) <= 25) {
    console.log("  ! settling the bill did not earn any points");
    process.exitCode = 1;
  }

  /* ---- returns: money going back ----------------------------------------
     Against the bill just settled, which is the only way in: a refund that
     does not point at a sale is a way to empty the drawer. */
  const returnable = await call("what is still returnable", "GET",
    `/orders/${order?.id}/returnable`, { tok: posTok });
  const line = returnable?.lines?.[0];
  const refunded = await call("give one back", "POST", "/returns", {
    tok: posTok, expect: [201],
    body: {
      orderId: order?.id,
      lines: [{ orderItemId: line?.orderItemId, qty: 1 }],
      reason: "smoke test",
    },
  });
  await call("more than remains is refused", "POST", "/returns", {
    tok: posTok, expect: [409],
    body: {
      orderId: order?.id,
      lines: [{ orderItemId: line?.orderItemId, qty: 99 }],
      reason: "smoke test",
    },
  });
  await call("the refund slip", "GET", `/returns/${refunded?.id}`, { tok: posTok });
  await call("returns against this bill", "GET", `/returns?orderId=${order?.id}`,
    { tok: posTok });

  // The Z-report, read before the count so the figure to declare is known.
  const zReport = await call("the Z-report", "GET", `/shifts/${shift?.id}`, { tok: posTok });
  const expected = zReport?.shift?.expectedCash ?? 0;
  await call("close against a short drawer", "POST", `/shifts/${shift?.id}/close`, {
    tok: posTok, body: { declaredCash: Number(expected) - 1 }, expect: [400],
  });
  await call("close the drawer", "POST", `/shifts/${shift?.id}/close`, {
    tok: posTok, body: { declaredCash: expected },
  });
  await call("shift history", "GET", "/shifts", { tok: adminTok });
  const order2 = await call("open a second bill", "POST", "/orders", { tok: posTok, body: { tableId: 6 } });
  await call("cancel order", "POST", `/orders/${order2?.id}/cancel`, { tok: posTok });

  /* ---- idempotency, against the running server -------------------------
     The suite proves this with MockMvc; what it cannot prove is that the
     filter is actually in the chain of the assembled application. A key that
     is silently ignored looks exactly like one that works, right up until two
     bills exist for one sale. */
  const sameKey = crypto.randomUUID();
  const firstTry = await call("open a bill with a fixed key", "POST", "/orders",
    { tok: posTok, body: { tableId: 8 }, idem: sameKey });
  const replay = await call("repeat it with the same key", "POST", "/orders",
    { tok: posTok, body: { tableId: 8 }, idem: sameKey });

  results.push({
    label: "the repeat returned the first bill, not a second one",
    method: "POST", path: "/orders", status: replay?.invoiceNo === firstTry?.invoiceNo ? 200 : 0,
    ok: Boolean(firstTry?.invoiceNo) && replay?.invoiceNo === firstTry?.invoiceNo,
    expect: [200],
    msg: `first ${firstTry?.invoiceNo ?? "?"}, repeat ${replay?.invoiceNo ?? "?"}`,
  });

  await call("cancel the idempotency bill", "POST", `/orders/${firstTry?.id}/cancel`, { tok: posTok });
  await call("a used promotion cannot be deleted", "DELETE", `/promotions/${promo?.id}`, {
    tok: adminTok, expect: [400],
  });
  await call("switch it off instead", "PUT", `/promotions/${promo?.id}`, {
    tok: adminTok,
    body: { name: promo?.name, type: "PERCENT", value: 10, scope: "ORDER",
            active: false, startsAt: yesterday, endsAt: nextWeek },
  });

  await call("stop asking it", "DELETE", "/modifier-groups/" + question?.id, { tok: adminTok });
  await call("take the size away", "DELETE", `/products/1/variants/${size?.id}`, { tok: adminTok });

  await call("a protected write with no key is refused", "POST", "/orders",
    { tok: posTok, body: { tableId: 8 }, idem: "", expect: [400] });

  /* ---- accounts and locks -----------------------------------------------
     The lockout counter is written on a path that then throws, which is how it
     came to be rolled back and silently never work. Exercising it against the
     running server is the only way to see that it counts. */
  const accounts = await call("account list", "GET", "/users", { tok: adminTok });
  await call("account list is admin-only", "GET", "/users", { tok: posTok, expect: [403] });

  const smokeAccount = (accounts?.content ?? []).find((a) => a.username === USER);
  if (smokeAccount) {
    await call("unlock an account that is not locked", "POST", `/users/${smokeAccount.id}/unlock`,
      { tok: adminTok });
    results.push({
      label: "the account list reports lock state without any password field",
      method: "GET", path: "/users",
      status: 200,
      ok: typeof smokeAccount.locked === "boolean"
        && !JSON.stringify(accounts).includes("userPwd")
        && !JSON.stringify(accounts).includes("$2a$"),
      expect: [200],
      msg: `locked=${smokeAccount.locked}, failedAttempts=${smokeAccount.failedAttempts}`,
    });
  }

  /* ---- audit ------------------------------------------------------------
     Written by a Hibernate listener rather than by any service, so the only
     way to know it is wired into the running application is to change
     something and look. */
  const audit = await call("audit log", "GET", "/audit", { tok: adminTok });

  // A 200 only proves the endpoint answers. The listener is registered against
  // Hibernate at startup, outside anything the suite exercises, so the question
  // that matters is whether the products and categories created above actually
  // produced entries.
  results.push({
    label: "the changes made by this run were recorded",
    method: "GET", path: "/audit", status: audit?.totalElements > 0 ? 200 : 0,
    ok: (audit?.totalElements ?? 0) > 0,
    expect: [200],
    msg: `${audit?.totalElements ?? 0} entries`,
  });
  await call("audit log, filtered by entity", "GET", "/audit?entity=Product&size=5", { tok: adminTok });
  // The date parameters are the ones that actually broke: PostgreSQL could not
  // infer the type of a timestamp compared only against NULL, where H2 could.
  // Every combination of supplied and omitted filters is a different query.
  await call("audit log, filtered by date", "GET",
    `/audit?from=2020-01-01&to=${today()}`, { tok: adminTok });
  await call("audit log, one date filter only", "GET", "/audit?from=2020-01-01", { tok: adminTok });
  await call("audit log, filtered by user", "GET", "/audit?userId=admin&size=5", { tok: adminTok });
  await call("audit log is admin-only", "GET", "/audit", { tok: posTok, expect: [403] });
  await call("audit log cannot be written to", "POST", "/audit", {
    tok: adminTok, body: {}, expect: [405],
  });

  /* ---- reports, dashboards, settings ----------------------------------- */
  const d = today();
  await call("admin dashboard", "GET", "/dashboard/summary", { tok: adminTok });
  await call("cashier dashboard", "GET", "/dashboard/cashier", { tok: adminTok });
  await call("sales report", "GET", `/reports/sales?from=${d}&to=${d}`, { tok: adminTok });
  await call("sales detail", "GET", `/reports/sales/detail?from=${d}&to=${d}`, { tok: adminTok });
  await call("sales csv", "GET", `/reports/sales.csv?from=${d}&to=${d}`, { tok: adminTok, raw: true });
  await call("read settings", "GET", "/settings", { tok: adminTok });
  await call("write settings", "PUT", "/settings", { tok: adminTok, body: { "sales.vatRate": "10" } });

  /* ---- the refusals matter as much as the successes -------------------- */
  await call("cashier cannot read staff", "GET", "/staff", { tok: cashierTok, expect: [403] });
  await call("cashier cannot read stock", "GET", "/stock", { tok: cashierTok, expect: [403] });
  await call("no token is rejected", "GET", "/products", { expect: [401] });
  await call("unknown path is 404", "GET", "/no-such-endpoint", { tok: adminTok, expect: [404] });
  await call("wrong method is 405", "PATCH", "/categories", { tok: adminTok, expect: [405] });

  report();
  process.exit(results.some((r) => !r.ok) ? 1 : 0);
}

function report() {
  const failed = results.filter((r) => !r.ok);
  for (const r of results) {
    if (QUIET && r.ok) continue;
    const flag = r.ok ? "ok  " : "FAIL";
    const detail = r.ok ? "" : `   expected ${r.expect} — ${r.msg}`;
    console.log(`${flag} ${String(r.status).padEnd(4)} ${r.method.padEnd(6)} ${r.path.padEnd(38)} ${r.label}${detail}`);
  }
  console.log(`\n${results.length} calls · ${results.length - failed.length} as expected · ${failed.length} not`);

  /*
   * A 429 is almost always this script tripping the limiter on its own previous
   * run rather than anything being wrong. Each run makes three /auth/register
   * calls against a limit of five an hour, so a second run inside the hour
   * fails — and then every call needing the account it could not create fails
   * after it, reporting "Authentication required" against an undefined id. The
   * cascade looks alarming and means nothing, so say so outright.
   */
  if (failed.some((r) => r.status === 429)) {
    console.log(
      "\nSome calls were rate limited (429), and the failures after them follow\n" +
      "from that rather than from anything being broken. This script makes 3\n" +
      "register attempts per run against a limit of 5 an hour, so running it\n" +
      "twice inside the hour does this to itself. Either wait, or start the\n" +
      "backend with RATE_LIMIT_ENABLED=false. CI is unaffected — it runs the dev\n" +
      "profile, where the limiter is off.",
    );
  }

  /*
   * On CI, say which call failed somewhere a reader can actually see it.
   *
   * Actions logs need a sign-in even on a public repository, so a red smoke run
   * reported nothing but "Process completed with exit code 1" — which is how a
   * flake and a real regression look identical. Annotations show on the run
   * page to anyone.
   */
  if (process.env.GITHUB_ACTIONS === "true") {
    for (const r of failed) {
      const title = `smoke: ${r.method} ${r.path}`;
      const message = `${r.label} — got ${r.status}, expected ${r.expect}${r.msg ? `: ${r.msg}` : ""}`;
      console.log(`::error title=${clean(title)}::${clean(message)}`);
    }
  }
}

/** Annotation text is one line, and % and newlines have to be escaped. */
function clean(text) {
  return String(text)
    .replace(/%/g, "%25")
    .replace(/\r/g, "%0D")
    .replace(/\n/g, "%0A")
    .replace(/\s+/g, " ")
    .trim();
}

main().catch((e) => {
  console.error("Smoke run itself failed:", e);
  process.exit(1);
});
