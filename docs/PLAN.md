# Step 5 — Build Plan

Closes the set: [ARCHITECTURE](ARCHITECTURE.md) · [SCREENS](SCREENS.md) ·
[ERD](ERD.md) · [API](API.md).

Baseline today: 15 tables, 76 endpoints, 22 screens, 19 backend test classes,
8 frontend test files, 4 CI jobs, 172 real orders in PostgreSQL.

---

## 1. The ordering principle, and its cost

Packages below are ordered by **how expensive each becomes if deferred**, not
by how much a user would notice. Adding `branch_id` to 172 orders is an
afternoon; adding it to 100,000 is a weekend with a maintenance window.

**The honest consequence: the first three packages are nearly invisible.** Two
weeks of work and the app looks identical. That is a real risk — for
motivation, and for anyone judging progress by screenshots.

> **Mitigation, and I would take it:** pull package **P5 (POS consolidation)**
> forward and do it after P1. It is frontend-only, needs no migration, removes
> a screen, and is the single most visible improvement in the plan. It buys
> room to do the invisible work without the project looking stalled.

---

## 2. Definition of done

Every package, no exceptions:

- [ ] Migration runs clean on PostgreSQL **and** on a copy of the real database
- [ ] Backend tests cover the new service logic; existing 19 classes still pass
- [ ] Frontend tests cover new components
- [ ] `smoke-api.mjs` extended to the new endpoints
- [ ] Khmer and English strings present; i18n CI job green
- [ ] All 4 CI jobs green before merge
- [ ] Main is deployable at the end of the package

**No long-lived branches.** Each package is days, not weeks, and merges green.
A six-week feature branch against a schema that is changing underneath it is
how this plan fails.

---

## 3. Packages

### P0 — Foundations · ~3 days · no migration risk

Nothing user-visible. Everything after is safer for it.

| | Work | State |
|---|---|---|
| a | ~~`dev` profile H2 → PostgreSQL~~ | **moved to P3** — see below |
| b | `code` field on `ApiResponse` (API §2) | **done** |
| c | `Idempotency-Key` filter + `idempotency_key` table | **done** (V6) |
| d | `audit_log` table + a Hibernate change listener | **done** (V7) |

**(a) was based on a wrong premise, found when it came to be done.** Three
facts, none of which this document knew:

- `bootRun` takes no profile argument, so the running application already uses
  the default profile — which points at PostgreSQL. Development was never on
  H2.
- `build.gradle` sets `spring.profiles.active=dev` on the `test` task. H2 is
  confined to `./gradlew test`, and `postgresTest` plus the CI `postgres` job
  already cover real PostgreSQL.
- There is no Docker on the development machine, so moving the default test
  task to Testcontainers would leave the suite unrunnable locally.

So the change would have delivered nothing and broken the local test loop.

**It becomes forced at P3.** The partial unique index that makes the
one-open-shift rule real (ERD §3.2) is PostgreSQL-only, and H2 rejects it in
the *migration* — so the whole suite fails at Flyway, completely rather than
gradually. That is the moment to decide, and by then whether Docker is
available will be known.

*Risk:* low. (d) touches every write path, so it was landed alone.

**(d) is a listener, not the aspect this document specified.** An aspect over
service methods records only what someone remembered to annotate, and it cannot
answer "from what, to what" without re-reading the row before the change.
Hibernate holds both states at flush time already, which is exactly the
question ERD §3.8 asks the log to answer. Entities opt in with `@Audited`;
orders, order lines and stock movements deliberately do not, because a day of
trading would bury every master-data change.

Two things the tests caught that the design had missed:

- A dining table's `status` flips on every order, so auditing it wrote 200 rows
  a day of noise. That is a different reason from hiding a password, so the
  annotation now separates `redact` (a secret; removing one is a security
  decision) from `ignore` (churn; removing one is a judgement about noise).
- `regId`/`regDtm`/`modId`/`modDtm` change on every update of every entity.
  They were putting two fields of bookkeeping beside each real change, and an
  update that touched nothing else still wrote a row saying "something was
  saved". They are now skipped for all audited entities. `actYn` is not, since
  a row being deactivated is a real change and one of the more interesting
  ones.

**And one the tests could not have caught.** The audit query was written as
`:param IS NULL OR column = :param`, which H2 accepts and PostgreSQL rejects:
*could not determine data type of parameter $7*. All 207 tests passed; the
live smoke run against the real database returned 500. It is now a
`Specification`, which emits only the predicates actually asked for, so the
question never arises.

That is the H2 divergence in §P0a arriving early and by itself — the
`postgresTest` task would have caught it, and there is no Docker here to run
it. The smoke script now covers every filter combination for this endpoint,
because those were the parameters that broke.

**(c) as built differs from ERD §3.8 in two ways**, both decided while writing
it:

- The row stores the finished response body, not a `response_ref` pointing at
  the created record. A reference needs per-endpoint logic to rebuild each
  reply and still would not reproduce one exactly; a body replays verbatim and
  does not care which endpoint produced it.
- A failed request **releases** its key rather than caching the failure. A
  cashier told "amount tendered is less than the total" corrects it and sends
  the same intent again — a cached rejection would answer the corrected request
  with the original complaint. It also stops one transient 500 making a key
  permanently unusable.

Requiring the header was a breaking change, as API §5 said it would be: 48
existing tests failed until they sent one. That is the correct direction —
protection a client can forget to ask for is not protection — but it means the
frontend and `smoke-api.mjs` had to move in the same commit.

### P1 — Multi-branch · ~4 days · **highest retrofit risk**

V6, V7. `company` and `branch` tables; `branch_id` on nine tables, `NOT NULL`,
backfilled to branch 1; `MANAGER` added to both role CHECKs; branch claim in
the JWT; `POST /api/auth/switch-branch`.

**Back up the database first.** 172 real orders.

*Risk:* high — it touches every query. But it is strictly cheaper now than at
any future point, which is the entire argument for doing it first.

*Visible result:* a branch badge in the header. That is all, and it is correct
that it is all.

### P5 — POS consolidation · **done**

SCREENS §2.1. Payment is a panel on `/cashier/order`; `/cashier/payment` is
deleted, along with its nav entry and the three strings only it used. Frontend
only, no migration, no API change.

Four screens per sale became two. The cashier screen count went **down**.

The tender arithmetic moved to `lib/payment.ts` as a pure function with its own
tests, following the precedent `landing.ts` set. `canPay` and the reason it is
disabled are derived together rather than as two expressions listing the same
conditions in the same order — which invites a disabled button with no reason
given, or a reason beside an enabled one.

Two bugs it turned up, both older than this package:

- `AWAITING_PAYMENT` was missing from the frontend's `OrderStatus`, so a bill
  with a live KHQR code fell through to the already-cancelled branch. A cashier
  watching a customer scan was told the bill had been cancelled.
- `Number(".")` is NaN, and the keypad allows a lone decimal point as the first
  press. It poisoned the change, the comparison and the message at once.

The panel holds one idempotency key for its lifetime, so a retry after a
timeout settles the same bill rather than a second one — the case P0c built the
mechanism for, and the first place needing a *stable* key rather than the
interceptor's per-request one.

### P2 — Money integrity · ~5 days

V9, V10 (partial), V11. `sale_payment`; `orders.fx_rate_khr` stamped at
settlement; `fx_rate` table; `order_item.unit_cost`.

**Two data-carrying migrations.** V9 must write a `sale_payment` row for every
paid order before dropping `payment_method`. Back up again.

**V10 backfills `unit_cost` from today's `product.cost`, and for historical
lines that is wrong** — a cost never recorded cannot be recovered. Label
pre-cutover margin as *estimated* in the reports UI and say so in the migration
comment. A silently plausible wrong number is worse than a labelled gap, and
this project has already been bitten once by a silent substitution.

*Risk:* high. Money, and irreversible history.

### P3 — Shift and cash · ~4 days

V8. `cash_shift`, `cash_movement`, the partial unique index, six endpoints,
`/cashier/shift`, and the **gate** — no open shift, no POS.

*Risk:* medium. The gate changes the daily routine of every cashier, so it
needs to be two taps or it will be worked around. Watch this one in use.

### P4 — Stock ledger · ~4 days

V10 (rest). Widen `stock_movement`; write one per sale line inside the settle
transaction; `product.stock_qty` becomes derived; `/admin/stock` reworked to
read-only levels + ledger + adjust modal.

*Risk:* medium. **Reconcile before and after** — current `stock_qty` values
must equal the ledger sum on day one, or every count is suspect from the start.

### P6 — Customers and loyalty · ~4 days
### P7 — Returns · ~5 days

Depends on P2, P3, P4, P6 — a return writes a payment reversal, a cash
movement, a stock movement and a loyalty reversal in one transaction. It is
last in Phase 1 because it needs all four to exist.

*Risk:* high. Returns are how money leaves the drawer.

### P8 — Variants and modifiers · ~5 days
### P9 — Promotions · ~4 days — percent and fixed only
### P10 — Navigation regroup · ~2 days

SCREENS §2.2. Five collapsible groups, persistent branch badge.

---

## 4. Phase 1 total

| Package | Days |
|---|---|
| P0 Foundations | 3 |
| P1 Multi-branch | 4 |
| P5 POS consolidation | 3 · **done** |
| P2 Money integrity | 5 |
| P3 Shift and cash | 4 |
| P4 Stock ledger | 4 |
| P6 Customers | 4 |
| P8 Variants | 5 |
| P9 Promotions | 4 |
| P7 Returns | 5 |
| P10 Navigation | 2 |
| **Total** | **43 days** |

**Read that as ~9 working weeks, and expect 12.** These are focused-day
estimates for one developer who knows the codebase. They contain no allowance
for the thing that actually happens — a migration that will not apply, a
Khmer string that clips, a test that fails only on CI. This project has already
spent real time on all three.

Phase 2 is not estimated here. Estimating work three months out is fiction.
§4a lists what is in it.

---

## 4a. Phase 2

Held deliberately, not forgotten. Nothing here fixes a number that is currently
wrong, which is what separates it from Phase 1.

| | Work | Waits on |
|---|---|---|
| Q1 | Kitchen display (SCREENS §3.9) | P8 — modifiers are most of what a cook reads |
| Q2 | **Staff attendance** — see below | P3 |
| Q3 | **Product photographs** — see below | nothing technical; someone to take them |
| Q4 | Supplier ledger; `supplier.balance` derived (ERD §1.3) | P2 |
| Q5 | Roles and permissions as data (ARCHITECTURE §5) | P1 |
| Q6 | Warehouse transfers | a second warehouse existing |
| Q7 | Buy-X-get-Y promotions | P7 — it interacts with returns |
| Q8 | Customer-facing display | a second monitor |

### Q2 — Staff attendance

`staff.shift` says a person is scheduled for mornings. `staff.status` can say
`ON_LEAVE`. Neither records that anyone actually turned up, and `staff.salary`
is a stored figure computed from nothing.

**Scope, deliberately small:**

- Record **raw facts only** — clock in, clock out, who recorded it. Compute
  nothing. A person works out pay from the hours.
- **Derive cashier attendance from `cash_shift`.** P3 already makes opening a
  shift a gate: no open shift, no POS. That `opened_at` is the cashier's
  arrival, recorded because they cannot sell without it. Asking them to clock
  in *as well* is two rituals for one arrival, and the one that is not enforced
  is the one people skip. Manual clock-in is for staff with no till — chefs,
  waiters, cleaners.
- **Corrections go through the audit log**, which P0d already built. Attendance
  a manager can edit silently is worthless the moment there is a dispute, and
  that is the only moment it matters.

**Explicitly not in scope:** leave balances, overtime calculation, payroll
export, approval workflows.

> **Why not more.** This is a point of sale, not an HR system. Attendance has
> real depth — leave, overtime rules, public holidays, late penalties — and
> Cambodia's overtime and holiday rules are specific enough that software
> computing pay wrongly is a liability rather than a feature. Recording hours
> and letting a person do the arithmetic is the honest version. Half-built
> attendance is worse than none, because a number on a screen gets trusted.

One table (`attendance`: staff, branch, clock-in, clock-out, source, note,
recorded-by), a "who is in now" board, and a per-staff month view. Roughly
3–4 days.

**Sequenced after P3, not before**, because P3 establishes the shift and
produces half the data — building attendance first means building it twice.
It is also not in the pasted brief; it is an addition, and worth naming as a
choice rather than a requirement.

---

### Q3 — Product photographs

V9 gave products an `icon` — one emoji, instant to render, backed up with
everything else by `pg_dump`. It is not enough, and the menu already shows why:

    U+2615   Coffee, Hot coffee, Milk coffee
    U+1F375  Lime tea, Steeped tea

Five of nineteen dishes cannot be told apart by their picture. The tile also
carries the dish name, so nothing is unidentifiable — but a picture that is
identical across three products has stopped doing the job a picture is for.

**Scope:** `image_path` *beside* `icon`, not instead of it. The emoji shows
while the photograph loads and stands in for any product without one, so a
missing or broken image degrades to what the screen does today rather than to
an empty box.

**What it actually costs**, listed because none of it is visible in "add an
upload button":

- Validation on content type **and** magic bytes. A `.jpg` extension on
  something that is not a JPEG is the oldest hole there is.
- Storage under UUID filenames, never the uploader's — a filename is caller
  input, and `../` is part of the alphabet.
- A resize step. The POS draws eighteen tiles at about 128px; phone photographs
  are megabytes each, and a till on a slow connection would feel it.
- A decision on whether photographs sit behind the access token or are public.
- Deletion that removes the file with the product, or orphans accumulate
  forever.
- **Backup.** This is the one to weigh hardest. The whole backup story today is
  a single `pg_dump`. Adding a directory makes it two things that must stay in
  step, and a restore from the dump alone would come back with every picture
  missing and nothing saying so.

Roughly 3–4 days, and someone has to photograph nineteen dishes — the part no
code does.

**Why it waits.** Nothing here is blocked by other work; it is behind Phase 1
because P4 fixes counts that are *wrong today* and cannot be backfilled, while
this makes a working screen nicer. That is the whole ordering principle in §1,
applied to a feature that is genuinely wanted.

---

## 5. CI must grow with it

Four jobs today: `backend` (H2 + smoke), `postgres` (Testcontainers),
`frontend`, `i18n`.

| Package | CI addition |
|---|---|
| P0 | idempotency replay test; envelope `code` asserted |
| P1 | **a job that runs migrations against a dump of the real database** |
| P2 | V9 data migration verified — every paid order gets exactly one payment |
| P3 | concurrent shift-open test proving the partial index holds |
| P4 | reconciliation test — ledger sum equals expected level |

**The P1 addition matters most.** CI currently proves migrations work on an
*empty* database. Every migration bug this project has hit was a bug on a
*populated* one. A nightly job restoring a sanitised dump and running Flyway
forward would have caught the V4 checksum mismatch before it reached the real
database.

---

## 6. What could go wrong

| Risk | Mitigation |
|---|---|
| P1 breaks a query that silently returns another branch's rows | Every repository method gets a branch-scoped test. There is no safe partial rollout |
| P2 loses payment data | Back up; V9 verified by count, not by inspection |
| V10 estimated margins mistaken for real | Label in the UI, not only in a comment |
| The shift gate gets worked around | Two taps, or it fails. Watch it in use in week one |
| Scope grows mid-package | The package list is the contract. New ideas go to Phase 2 |
| Estimates slip and Phase 1 never lands | P0–P5 alone is a coherent release. Ship there if needed |

**If time runs short, stop after P4.** Foundations, multi-branch, a consolidated
POS, real payments, shifts and a stock ledger is a genuinely better system than
today's and it stands on its own. Customers, variants, promotions and returns
are additive. Stopping mid-P7 is not — a half-built return path is worse than
none.

---

## 7. Decisions still open

Blocking, in the order they bite:

1. **`dev` off H2 onto PostgreSQL?** — P0a. Decides whether the one-open-shift
   rule is a database guarantee or a service-layer hope.
2. **`code` on the response envelope?** — P0b. One line in `ApiResponse`.
3. **Historical margin labelled estimated?** — P2. Affects what reports say.
5. **Offline scope: render-only, queue nothing?** — SCREENS §6.
6. **`orders` keeps its name** rather than becoming `sale`? — ERD §2.

Not blocking, and worth revisiting at Phase 2: Redis, WebSocket, the load
balancer, GitLab CI. ARCHITECTURE §3.2 argues for deferring all four; nothing
in Steps 2–5 has changed that.

---

## 8. Where I would start on Monday

1. Back up the database. Verify the backup restores — an unverified backup is a
   belief, not a backup.
2. P0a: point `dev` at PostgreSQL, run the whole suite, fix what H2 was hiding.
3. P0b: add `code` to `ApiResponse`, assert it in one test.
4. Then P0c and P0d, then stop and reassess before P1.

Step 1 of that list is worth doing today regardless of whether any of this plan
is adopted.
