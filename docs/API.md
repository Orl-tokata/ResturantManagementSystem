# Step 4 — API Specification

Follows [ARCHITECTURE.md](ARCHITECTURE.md), [SCREENS.md](SCREENS.md),
[ERD.md](ERD.md).

76 endpoints exist across 11 controllers. This step adds 41, changes 6, and
removes none.

---

## 1. Conventions already in force — keep all of them

**Envelope**, every endpoint, success and failure alike:

```json
{ "status": 200, "message": "OK", "data": { }, "timestamp": "2026-09-29T20:16:04" }
```

**Errors** localise server-side. `ApiException` carries a *message key* plus
arguments, and `GlobalExceptionHandler` — the one place that knows the
request's locale — resolves it. A service picking a language would be wrong,
and this already avoids it.

**Auth**: Bearer access token in memory; refresh token in an httpOnly cookie
scoped to `/api/auth`. `PUBLIC_PATHS` is an explicit allow-list, never a
wildcard.

**Paging**: `PageResponse<T>`, `?page=&size=`.

---

## 2. One change worth making before anything else

### The error code is computed and then thrown away

`ApiException` holds `messageKey`. The handler resolves it to text and the
envelope carries only the sentence. **The client gets prose it cannot branch
on.** A frontend wanting to react differently to "table already occupied" and
"insufficient stock" has nothing to test but a translated string — and it
changes when the locale does.

> **Add `code` to the envelope.** The value already exists; it is being dropped
> at the boundary.
>
> ```json
> { "status": 409, "code": "order.table.occupied",
>   "message": "តុនេះមានភ្ញៀវរួចហើយ", "data": null, "timestamp": "…" }
> ```
>
> Purely additive — no existing client breaks — and it is the difference
> between a frontend that can recover from a conflict and one that can only
> show a toast. With returns, shifts and split payment arriving, the number of
> recoverable conflicts is about to multiply.

`code` is `null` for successes, or `"OK"`. Do not invent a parallel numbering
scheme; the message key *is* the code.

---

## 3. Branch scoping — server-derived, never a parameter

Every new endpoint is branch-scoped. The branch comes from **the
authenticated user**, not the request.

```
GET /api/orders?branchId=3     ← never. Not as a query param, not in a body.
```

> This system has already shipped one privilege-escalation bug of exactly this
> shape: registration honoured a client-supplied role until it was fixed. A
> client-supplied `branchId` is the same mistake wearing a different hat — read
> another shop's takings by changing a number in a URL.

For the multi-branch user of SCREENS §3.6, switching branch is an **auth**
operation, not a query parameter:

```
POST /api/auth/switch-branch    { "branchId": 3 }   → new access token
```

The server checks membership, then mints a token carrying the new branch. The
claim is signed; the parameter was not. Every subsequent request is scoped by
the token, and no endpoint needs to think about it.

`ADMIN` may pass `?branchId=` on **reporting endpoints only**, checked against
membership — an owner comparing two shops is a real need, and reports are
read-only.

---

## 4. Idempotency

```
POST /api/orders/{id}/payments
Idempotency-Key: 1f4c2b7e-...
```

A header, not a body field — it is metadata about the request, not about the
payment, and the same key must work across endpoints.

Required on: create order, add payment, return, stock adjust, shift close.
Optional elsewhere. Missing on a required endpoint → `400 idempotency.required`.

Replay of the same key returns the **original response** with `200`, not a
second resource and not a `409`. A cashier who taps twice on bad Wi-Fi should
see one bill and no error — the error would be a lie, since their intent
succeeded.

---

## 5. Changed endpoints

| Endpoint | Change | Breaking? |
|---|---|---|
| `POST /api/orders/{id}/pay` | kept as sugar for the single-payment case | no |
| `GET /api/orders/{id}` | `payments[]` added; `paymentMethod` retained, derived | no |
| `POST /api/orders` | accepts `customerId`, `shiftId`; `Idempotency-Key` required | **yes** |
| `PUT /api/orders/{id}/items` | items accept `modifiers[]`, `variantId` | no |
| `GET /api/stock/{id}/movements` | gains `refType`, `refId`, `balanceAfter` | no |
| `POST /api/stock/{id}/adjust` | `reason` becomes mandatory | **yes** |

**`POST /orders/{id}/pay` stays.** The overwhelming majority of bills are one
cash payment, and making every till do a two-step dance to serve the split-
payment minority is the wrong trade. It becomes a thin wrapper that posts one
payment and settles — one endpoint, one round trip, unchanged for clients.

**`paymentMethod` stays on the response** as a derived field: the single method
when there is one, `"SPLIT"` when there are several. The receipt template, the
history list and two report queries all read it, and none of them care about
the breakdown.

---

## 6. New endpoints

### 6.1 Shift — 6

| | |
|---|---|
| `GET /api/shifts/current` | open shift for the caller, or `204` |
| `POST /api/shifts` | open. `{openingFloat}` |
| `GET /api/shifts/{id}` | detail with movements |
| `POST /api/shifts/{id}/close` | `{declaredCash, note}` → Z-report |
| `GET /api/shifts/{id}/report` | Z-report, reprintable |
| `POST /api/shifts/{id}/cash-movements` | pay-in, pay-out, drop |

`GET /shifts/current` returning `204` rather than `404` is deliberate: no open
shift is a normal state at 7am, not an error, and the frontend gate
(SCREENS §3.1) reads this on every POS load.

`POST /api/shifts` when one is already open → `409 shift.already.open`, and
the partial unique index of ERD §3.2 is what actually guarantees it.

### 6.2 Payments — 4

| | |
|---|---|
| `POST /api/orders/{id}/payments` | add one. Idempotent |
| `GET /api/orders/{id}/payments` | list |
| `DELETE /api/orders/{id}/payments/{pid}` | void an uncaptured payment |
| `POST /api/orders/{id}/settle` | close when covered |

`DELETE` only removes a payment that has not been captured. A captured payment
is reversed by a **return**, never deleted — that distinction is the difference
between an audit trail and a hole in one.

The existing three KHQR endpoints are unchanged and now write a `sale_payment`
row on confirmation instead of stamping the order.

### 6.3 Returns — 4 · **built (P7)**

| | |
|---|---|
| `GET /api/orders/{id}/returnable` | lines with quantity still returnable |
| `POST /api/returns` | create. Idempotent |
| `GET /api/returns` | list, filterable |
| `GET /api/returns/{id}` | detail + printable slip |

`/returnable` exists because no database constraint can prevent over-returning
across two documents (ERD §3.7). The server computes what remains; the client
never subtracts for itself. Posting more than remains → `409 return.exceeds.sold`.

### 6.4 Customers and loyalty — 7 · **built (P6)**

```
GET    /api/customers                ?q= name or phone
GET    /api/customers/lookup?phone=  single, for the POS
POST   /api/customers
GET    /api/customers/{id}
PUT    /api/customers/{id}
DELETE /api/customers/{id}
GET    /api/customers/{id}/loyalty   ledger; balance is its sum
POST   /api/customers/{id}/loyalty   manual adjust, ADMIN
```

`/lookup` is separate from `/customers?q=` because the POS case is different:
one exact phone, one result or none, hit on every sale. A list endpoint with
paging metadata is the wrong shape for a field a cashier tabs through.

> **Two more shipped, and why.** This list assumes the customer is known when
> the bill opens. At a table they are not: the question is asked at the point
> of paying, by which time the bill exists. So `PUT /orders/{id}/customer`
> names or clears them on an open bill, and `GET /orders` takes a
> `customerId` so the detail page can show somebody's purchases without a
> second listing endpoint.
>
> `/lookup` returns an **array**, not one customer. The phone column is not
> unique on purpose (ERD §3.4) and a couple sharing a number is ordinary, so
> the till shows both rather than guessing.

### 6.5 Catalog extensions — 8

```
GET|POST         /api/products/{id}/variants
PUT|DELETE       /api/products/{id}/variants/{vid}
GET|POST         /api/modifier-groups
PUT|DELETE       /api/modifier-groups/{id}
POST|DELETE      /api/products/{id}/modifier-groups/{gid}
GET              /api/products/barcode/{code}
```

`GET /products/barcode/{code}` returns one product or `404`. Scanner input is
a lookup, not a search — ARCHITECTURE §1.3 says the scanner types into the
existing search box, and this is the endpoint that box calls when the input
looks like a barcode.

### 6.6 Promotions — 5

```
GET|POST   /api/promotions
GET|PUT|DELETE /api/promotions/{id}
GET        /api/promotions/applicable?orderId=
```

`/applicable` is computed **server-side**. A client that decides its own
discount decides its own price, and ARCHITECTURE §4.2 already establishes that
the server recomputes every total and never trusts the frontend's arithmetic.
This endpoint exists so the UI can *show* what will apply — the authoritative
application still happens inside the settle transaction.

### 6.7 Organisation — 7

```
GET|POST       /api/branches
GET|PUT|DELETE /api/branches/{id}
GET            /api/branches/mine       branches the caller may switch to
GET|POST       /api/users
GET|PUT|DELETE /api/users/{id}
POST           /api/users/{id}/reset-password
GET            /api/audit                filterable, read-only
```

`/api/users` is separate from `/api/staff`. Staff is an HR record and does not
imply a login; this distinction already exists in the code and was misstated
once in this project's history, so it is worth keeping the endpoints visibly
apart.

**`/api/audit` has no POST, PUT or DELETE.** Not "not yet" — never. Writes
happen as a side effect of the operations being audited.

### 6.8 Kitchen — 3

```
GET   /api/kitchen/orders               NEW and COOKING, oldest first
PATCH /api/kitchen/orders/{id}/status   advance
PATCH /api/kitchen/items/{id}/status    per-line, for split-timing kitchens
```

Polling until WebSocket (ARCHITECTURE §7). `GET` supports
`If-None-Match`/`ETag` so a 3-second poll on a quiet kitchen costs a `304` and
nearly nothing — the cheap version of realtime, and the reason deferring
WebSocket is not painful.

### 6.9 Reference — 4

```
GET|POST /api/fx-rates
GET      /api/fx-rates/current
GET      /api/suppliers/{id}/ledger
```

---

## 7. Permissions

Until roles are data (ARCHITECTURE §5) these stay `@PreAuthorize` annotations.

| Area | ADMIN | MANAGER | CASHIER | WAITER | CHEF |
|---|---|---|---|---|---|
| POS, orders, payments | ✓ | ✓ | ✓ | ✓ | |
| Own shift | ✓ | ✓ | ✓ | | |
| Returns | ✓ | ✓ | limited | | |
| Kitchen | ✓ | ✓ | | | ✓ |
| Catalog, promotions | ✓ | ✓ | | | |
| Stock, purchases | ✓ | ✓ | | | |
| Reports | ✓ | ✓ | own shift | | |
| Users, branches, audit | ✓ | | | | |

`MANAGER` does not exist yet — ERD V7 adds it to the CHECK constraints on both
`users_infm` and `staff`. Until then its column is ADMIN's.

"Limited" for cashier returns means: up to a configured amount, above which
`403 return.approval.required` and a manager's credentials are needed.

---

## 8. Things I would not build

| Asked for | Instead |
|---|---|
| `GET /api/sales` as a new name for orders | `orders` keeps its name — ERD §2 |
| Client-supplied `branchId` | §3 |
| `PATCH` for partial updates everywhere | `PUT` with the full resource, as now. Mixing both doubles the validation paths for no gain at this size |
| GraphQL | 41 new endpoints is not a client-flexibility problem |
| API versioning (`/api/v2/…`) | One client, deployed together. A version prefix is a promise to maintain two servers |

The two breaking changes in §5 are safe **because the only client ships with
the server**. That is worth stating, since it stops being true the moment
anything else calls this API — and at that point §5 would need a deprecation
window instead.

---

## 9. Endpoint count

| | Now | After |
|---|---|---|
| auth | 10 | 11 |
| catalog | 12 | 20 |
| orders + payments + returns | 14 | 22 |
| stock + purchase + supplier | 22 | 23 |
| shifts | 0 | 6 |
| customers | 0 | 7 |
| promotions | 0 | 5 |
| organisation + audit | 5 | 12 |
| kitchen | 0 | 3 |
| reports + settings + health | 8 | 9 |
| **Total** | **76** | **117** |

---

## Next

Step 5 is the build plan — sequencing, migration safety, and what Phase 1
actually contains.

Two decisions from Step 3 still open and still blocking:

1. **`dev` profile off H2 onto PostgreSQL?** Decides whether the one-open-shift
   rule (§6.1) is a database guarantee or a service-layer hope.
2. **Historical margin labelled as estimated** after the V10 backfill.

New here, and worth an explicit yes: **adding `code` to the response envelope**
(§2). It is a one-line change to `ApiResponse` and a small one to
`GlobalExceptionHandler`, and every endpoint below is easier to consume with
it than without.
