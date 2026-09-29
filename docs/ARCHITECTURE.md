# Step 1 — System Architecture

Cambodia POS, restaurant-first. Written against the system that exists in this
repository rather than a blank page.

---

## 0. The assumption this rests on

The brief describes a **retail / mini-mart POS** and asks for it to be
extensible to restaurants later. This repository is already a **working
restaurant system**: 17 backend modules, 15 tables, 76 endpoints, 187 backend
tests, CI green, running against PostgreSQL with KHQR payment already verified
server-side.

So I am reading the request as **evolve this system towards the brief**, not
start again. If the intention really is a separate greenfield retail product,
stop here and say so — the two paths diverge immediately and almost nothing
below applies to the greenfield one.

Everything else in this document follows from that assumption.

---

## 1. Where the brief and a restaurant disagree

The brief is retail-shaped. Four places where following it literally would make
this system **worse**, and what to do instead.

### 1.1 The POS flow is not a cart

The brief's flow is `scan → add → pay → print`. That is a mini-mart: one
customer, one basket, one moment.

A restaurant bill is **open over time**. A table is seated, items arrive across
an hour, the bill may be split, and settlement happens at the end. This system
already models that — `orders` has a status of `OPEN`, a `table_id`, and items
appended as they are ordered.

> **Keep the open-bill model.** Add the retail fast path later as a second
> entry point (a "quick sale" with no table), not by replacing what is there.
> A mini-mart sale is a restaurant bill that opens and closes in one action;
> the reverse is not true.

### 1.2 Variants vs modifiers

The brief asks for product variants — T-Shirt / Small / Black. That is retail.

A restaurant needs **modifiers**: no ice, extra spicy, takeaway box. These are
different shapes. A variant is a distinct sellable thing with its own SKU,
barcode and stock. A modifier attaches to a line item, may change the price,
and has no independent stock.

> Build `product_variant` as the brief specifies — sizes are real (small/large
> coffee). But add `product_modifier` and `order_item_modifier` as well, and do
> not try to express "no ice" as a variant. Doing so multiplies the menu
> combinatorially for something that is a property of one line.

### 1.3 Barcode scanning

Central to the brief, near-useless for prepared food. Dishes have no barcode.

> Keep barcode on `product` — packaged drinks and retail add-ons have them, and
> the receipt barcode already built uses Code 128. But the POS search must stay
> name-and-category first, which is what it does today.

### 1.4 Stock

A mini-mart sells the thing it bought. A restaurant buys ingredients and sells
dishes, and the link between them is a recipe.

This system deliberately shipped without recipes (PROJECT-SPEC §12): paying
decrements the dish's own `stock_qty` and writes no movement. That is a known,
recorded decision, not an oversight — but see §6, because it conflicts with the
brief's rule 7 and with the brief's own inventory model.

---

## 2. Logical architecture

A **modular monolith**, per the brief's rules 1 and 2. Nothing here argues for
microservices, and a single restaurant will not need them.

```
┌──────────────────────────────────────────────────────────────┐
│  Next.js (App Router, TypeScript, Tailwind)                  │
│                                                              │
│  (protected)/admin/*     back office, ADMIN only             │
│  (protected)/cashier/*   POS, any signed-in role             │
│  (auth)/*                login, password reset               │
│                                                              │
│  lib/api.ts  ── axios, access token in memory only           │
└───────────────┬──────────────────────────────────────────────┘
                │ HTTPS, Bearer access token
                │ httpOnly refresh cookie, scoped /api/auth
┌───────────────▼──────────────────────────────────────────────┐
│  Spring Boot 3.5 · Java 17                                   │
│                                                              │
│  Edge        RateLimitFilter → SecurityContextHolderFilter    │
│              → JwtAuthenticationFilter → @PreAuthorize        │
│                                                              │
│  Modules     auth user staff  │  catalog dining order         │
│              setting report    │  stock purchase supplier     │
│              khqr  security    │  common config health        │
│                                                              │
│  Each module: Controller → Service → Repository → Entity      │
│               DTOs at the edge. No business logic in a        │
│               controller. (Brief rule 17.)                    │
└───────────────┬──────────────────────────────────────────────┘
                │ JDBC
┌───────────────▼──────────────────────────────────────────────┐
│  PostgreSQL 16 · schema owned by Flyway, ddl-auto=validate   │
└──────────────────────────────────────────────────────────────┘
                │ HTTPS, server-to-server, token never in browser
┌───────────────▼──────────────────────────────────────────────┐
│  Bakong Open API — KHQR settlement verification              │
└──────────────────────────────────────────────────────────────┘
```

**What is already built:** everything in the Spring Boot and PostgreSQL boxes,
and the Bakong boundary. The modules listed are the real package names.

---

## 3. Deployment architecture

### 3.1 What to deploy now

```
                  ┌────────────┐
   browser ──443──│   Nginx    │──── / ────────► Next.js  :3000
                  │  TLS here  │──── /api ─────► Spring    :8081
                  └────────────┘
                                                    │
                                              PostgreSQL :5432
```

One box, three containers plus a database. Nginx terminates TLS and is the only
thing listening publicly — which is also what finally lets `cookie.secure=true`,
currently `false` because development runs over plain HTTP.

### 3.2 What the brief asks for, and why to wait

The brief asks for Redis, WebSocket, and a load balancer across WAS1/WAS2.

> **Recommend deferring all three.** The brief's own rules 1–3 say prefer simple
> architecture, start with a modular monolith, do not over-engineer the MVP.
>
> Redis Pub/Sub exists in the brief to fan WebSocket events across instances.
> With one instance there is nothing to fan. A second instance is what makes it
> necessary, and one restaurant on one box will not have one.
>
> The cost of waiting is near zero **provided the seam is kept**: see §7.

There is one thing Redis would earn today, and it is not events —
`RateLimiter` holds its buckets in a `ConcurrentHashMap`, so a second instance
would allow the limit twice over. That is already noted in PROJECT-SPEC §12.
When a second instance arrives, `RateLimiter.tryConsume` is the one method to
move behind Redis.

### 3.3 CI

The brief asks for GitLab CI. This repository has working GitHub Actions: four
jobs, including one that runs the whole suite against a real PostgreSQL in
Testcontainers, and one that boots the assembled jar and exercises 87 endpoints
against it.

> **Recommend keeping GitHub Actions** unless the hosting is genuinely moving to
> GitLab. Porting working CI is churn that buys nothing. The pipeline stages
> are the same either way.

---

## 4. Communication flow

### 4.1 Normal request — REST, and it stays REST

```
browser ──► Nginx ──► Spring ──► service ──► repository ──► Postgres
        ◄──────────── ApiResponse<T> envelope ◄────────────
```

Every response uses the existing envelope: `{status, message, data, timestamp}`.
Errors localise from `Accept-Language`, because a message the server refuses
with cannot be translated on the client.

### 4.2 Settling a bill — one transaction

The brief's rule 21 requires this to be atomic. Today `OrderService.pay` does:

```
@Transactional
  recalculate            ← server recomputes; frontend totals are never trusted
  validate tender        ← cash must cover the total
  mark PAID, stamp time
  decrement stock
  free the table
  save
```

What must join that transaction as the brief's features land: `sale_payments`
rows for split payment, a `stock_movement` per line, a `loyalty_transaction`,
and a `cash_movement` against the open shift. All inside the same
`@Transactional` boundary, so a partial settlement cannot exist.

### 4.3 KHQR — already built this way

```
POST /orders/{id}/khqr    generate, park the order in AWAITING_PAYMENT
                          same code returned if asked again
GET  /orders/{id}/khqr    ask Bakong; only PAID changes anything
DELETE /orders/{id}/khqr  abandon, return to OPEN so cash can settle it
```

`AWAITING_PAYMENT` is the point: a code on a screen is a request for money, and
only the bank saying so turns it into a payment. An unreachable bank returns
`UNKNOWN` and the order does not move — guessing "unpaid" strands a paying
customer, guessing "paid" gives away food.

---

## 5. Security boundaries

| Boundary | Control | State today |
|---|---|---|
| Browser → Nginx | TLS | Nginx not yet deployed; `cookie.secure=false` until it is |
| Unauthenticated → API | `PUBLIC_PATHS` allow-list, everything else denied | Built. Listed one by one, never `/api/auth/**` |
| Brute force | Token-bucket rate limiter ahead of the security chain | Built. login 10/min, OTP 10/10min, register 5/hr |
| Authenticated → admin | `@PreAuthorize("hasRole('ADMIN')")` + client route guard | Built |
| Role escalation | Account creation is admin-only | Built — it was a public endpoint honouring a client-supplied role until it was fixed |
| Server → Bakong | Token server-side only, never reaches the browser | Built |
| Financial values | Server recalculates everything | Built for orders; must extend to split payments |

**Not yet built, and required by the brief:**

- **A real audit log.** `reg_id` / `mod_id` / `reg_dtm` / `mod_dtm` on rows are
  provenance columns, not an audit trail. They cannot answer "who changed this
  price, from what, to what, when". That needs its own append-only table.
- **Idempotency** on sale and payment writes. A cashier double-tap or a retried
  request can currently create two bills. This matters more than most of the
  feature list and is cheap now, expensive later.
- **Permissions as data.** Roles are a Java enum. The brief wants
  `roles`/`permissions`/`user_roles` tables so an owner can say "this cashier
  may discount up to 10%" without a deploy.

---

## 6. Inventory — the brief contradicts what is here

Brief rule 7: keep inventory movement history. Brief §6: every movement records
product, variant, warehouse, quantity, type, reference, user, timestamp.

Today:

- `stock_movement` exists — but only for `stock_item`, the ingredient table. It
  has `stock_item_id, movement_type, qty, reason, created_by, created_at`. No
  warehouse, no reference to the document that caused it, no product.
- **Selling a dish writes no movement at all.** It mutates `product.stock_qty`
  directly. That is exactly the "mutable quantity without history" the brief
  forbids.

> This is the largest single gap between the brief and reality, and it is a
> correctness gap rather than a feature gap: today there is no way to answer
> "why is this count wrong", because nothing recorded the decrements.
>
> **Recommend fixing this early**, before more sales accumulate. A movement
> table is easy to start and impossible to backfill.

---

## 7. WebSocket and Redis — design the seam, defer the parts

Do not build either now. Do make sure neither is painful to add.

**The seam is a domain event.** When something happens that a screen might want
to know about, publish an event rather than calling a notifier:

```java
// today: nobody listens, and nothing is lost
applicationEventPublisher.publishEvent(new OrderPaid(orderId, branchId, total));
```

Spring's own publisher, in-process, no dependency. Later, a listener turns
these into WebSocket frames; later still, a Redis Pub/Sub listener relays them
between instances. Neither change touches `OrderService`.

**What genuinely wants realtime, in order of value:**

1. **Kitchen display** — a cook needs to see an order without refreshing. This
   is the one that actually justifies WebSocket, and it is Phase 3.
2. **Multi-terminal table state** — two tills must not seat the same table.
3. **KHQR settlement** — already solved by polling every 3s while a code is
   live. WebSocket would save a handful of requests during the seconds a
   customer is paying. **Not worth an infrastructure tier on its own.**
4. Low-stock alerts — a banner on next load is enough.

---

## 8. KHQR integration boundary

Built, and already provider-independent in the way the brief asks.

```
OrderService ──► KhqrGenerator  (NBC/EMVCo spec: TLV, CRC-16/CCITT, MD5)
             ──► BakongClient   (check_transaction_by_md5)
                      │
                      └── PaymentStatus { PAID · NOT_PAID · UNKNOWN · UNVERIFIABLE }
```

`PaymentStatus` is the boundary. `OrderService` knows those four states and
nothing about Bakong. A second provider implements the same contract; nothing
in sales logic changes — which is brief rule 4 and rule 10 together.

`UNVERIFIABLE` is worth keeping when the abstraction generalises: it means no
token is configured, so the code scans and a customer can really pay, but the
till cannot learn that they did and says so rather than spinning.

**Honest limit:** the client is tested against a mock of Bakong's documented
responses. There is no merchant account here, so the first call against the
real API is still a first call.

---

## 9. What Phase 1 should actually be

The brief's Phase 1 lists 17 items. Eight are already done. Re-ordered by what
is most expensive to retrofit:

| | Work | Why now |
|---|---|---|
| 1 | **Multi-branch** — `company_id` / `branch_id` on every business table | Trivial with 172 orders, painful with 100,000. `bizKey` already exists on users and scopes nothing |
| 2 | **Movement-based inventory** | §6. Cannot be backfilled |
| 3 | **Idempotency keys** on sale and payment | Prevents duplicate bills; cheap now |
| 4 | **Audit log table** | Append-only history cannot be reconstructed later |
| 5 | **Cashier shifts** + cash movements | Blocks the cash-drawer reports |
| 6 | **Split payment** — `sale_payments` | Changes the payment model; do before promotions depend on it |
| 7 | **Customers** + loyalty transactions | Needed by returns and promotions |
| 8 | **Returns / refunds** | Depends on 2, 6, 7 |
| 9 | Permissions as data | Replaces the `Role` enum |
| 10 | Exchange-rate history | Today `currency.khrRate` is a single mutable setting; a reprinted receipt would use today's rate for last month's sale — a real correctness bug |

**Already built:** auth, roles (as an enum), dashboard, POS, products,
categories, ingredient stock, sales history, cash payment, KHQR architecture,
basic reports, currency display, receipt, i18n.

---

## 10. Risks worth stating plainly

1. **Retail-first framing applied to a restaurant** would replace an open-bill
   model with a cart. §1.
2. **Redis + WebSocket + load balancing before a second instance exists** is
   cost with no return, and the brief's own rules 1–3 argue against it. §3.2.
3. **The mutable `product.stock_qty`** contradicts the brief's inventory model
   and cannot be repaired retrospectively. §6.
4. **No idempotency** on financial writes. A retried request makes a second
   bill today.
5. **A single mutable exchange rate** silently rewrites history on reprints.
6. **Roles as an enum** means every permission change is a deploy.
7. **No audit log**, despite the brief calling it mandatory.
8. **Seeded credentials** (`ChangeMe123!`) are documented in a public
   repository. Rotate before anything real.

---

## Next steps

Step 1 is this document. Steps 2–5 — screen list, ERD, API specification,
task plan — follow once the assumption in §0 is confirmed and the deferrals in
§3.2 are either accepted or overruled.

Those four steps must agree with each other before any of them is implemented,
which is the brief's own instruction and the right one.
