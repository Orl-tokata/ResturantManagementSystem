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
 * This is not a replacement for the test suite — those 166 tests cover the
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

async function call(label, method, path, { tok, body, expect = [200, 201], raw = false } = {}) {
  const headers = {};
  if (tok) headers.Authorization = "Bearer " + tok;
  if (body !== undefined) headers["Content-Type"] = "application/json";

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
  await call("refresh without cookie", "POST", "/auth/refresh", { expect: [400] });
  await call("register", "POST", "/auth/register", {
    body: {
      username: USER, password: "Passw0rdX", fullName: "Smoke Test",
      email: `${USER}@rms.local`, role: "CASHIER",
    },
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
  const posTok = await login(USER, "Passw0rdY");
  await call("list orders", "GET", "/orders", { tok: posTok });
  await call("order summary", "GET", "/orders/summary", { tok: posTok });
  const order = await call("open a bill", "POST", "/orders", { tok: posTok, body: { tableId: 5 } });
  await call("order by id", "GET", `/orders/${order?.id}`, { tok: posTok });
  await call("open order for a table", "GET", "/orders/open?tableId=5", { tok: posTok });
  await call("set order items", "PUT", `/orders/${order?.id}/items`, {
    tok: posTok, body: { items: [{ productId: 1, qty: 2 }, { productId: 11, qty: 1 }] },
  });
  await call("take payment", "POST", `/orders/${order?.id}/pay`, {
    tok: posTok, body: { paymentMethod: "CASH", amountTendered: 50 },
  });
  await call("fetch receipt", "GET", `/orders/${order?.id}/receipt`, { tok: posTok });
  const order2 = await call("open a second bill", "POST", "/orders", { tok: posTok, body: { tableId: 6 } });
  await call("cancel order", "POST", `/orders/${order2?.id}/cancel`, { tok: posTok });

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
}

main().catch((e) => {
  console.error("Smoke run itself failed:", e);
  process.exit(1);
});
