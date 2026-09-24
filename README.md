# Restaurant Management System

Spring Boot REST API + Next.js frontend, built from the clickable prototype in
`../Prototype-Restaurant-mgs`.

📄 **Start here → [PROJECT-SPEC.md](PROJECT-SPEC.md)** — domain model, API contract,
screen map, and the 14-milestone build order.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5.6 · Java 17 · Spring Security + JWT · Spring Data JPA |
| Database | PostgreSQL 16 · Flyway migrations |
| Frontend | Next.js (App Router) · TypeScript · Tailwind CSS · React Query |
| Docs | Swagger / springdoc-openapi |

## Quick start

No database required — the `dev` profile runs on in-memory H2:

```bash
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'
```

```bash
cd frontend && npm run dev
```

Open <http://localhost:3000> — the page reports whether it can reach the API.

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API health | http://localhost:8081/api/health |
| Swagger | http://localhost:8081/swagger-ui.html |
| H2 console (`dev` only) | http://localhost:8081/h2-console |

For PostgreSQL instead of H2, see [PROJECT-SPEC.md §9](PROJECT-SPEC.md).

## Status

**All 14 milestones complete and verified.**
Scaffold · Schema · Auth backend · Auth frontend · App shell · UI kit ·
Master data · POS · Payment · History · Supply chain · Reports · Settings · Hardening

The **whole cashier flow works**: table → order → payment → receipt → history,
with the receipt reachable again from any past bill.

The **supply chain works too**: raise a purchase order, receive the goods, and
stock rises, a movement is recorded per line, unit costs refresh and the
supplier's payable goes up — all in one transaction.

👉 **Ring up a real sale:** sign in as `cashier` → `/cashier/tables` → pick a
table → tap dishes → **Pay** → choose cash, enter what the customer handed over
→ **Confirm**. You get a printable receipt, the stock drops and the table frees.

👉 **Working CRUD screens:** `/admin/categories` · `/admin/products` ·
`/admin/tables` · `/admin/staff` — real data from the API, add/edit modals,
delete confirmation, debounced search, filters and pagination.
Component gallery at `/admin/ui-kit`.

- `./gradlew build` passes; `/api/health` returns `status: UP`, `database: UP`
- Flyway applies V1–V3; Hibernate `ddl-auto=validate` passes, so the entity
  mappings provably match the migrations
- `./gradlew test` — **138 tests, 0 failures, 0 skipped** (on H2)
- POS verified over real HTTP: open bill → table becomes OCCUPIED → re-opening
  reuses the same bill → items priced and taxed (15.00 + 10% = 16.50 = 67,650៛)
  → recovered after reload → cancel frees the table and locks the bill
- Master data verified over real HTTP: CRUD round-trip returns 201/200/200/404,
  a cashier gets 403 on writes and on `/api/staff`, anonymous gets 401, and the
  delete guards answer 409 with a specific message
- Auth verified over real HTTP: login → token, `/me` 401 without it and 200 with
  it, refresh cookie exchanged for a fresh access token, other routes 401
- History verified over real HTTP: 3 paid + 1 cancelled + 1 open →
  summary reports sales 29.70, paid 3, avg 9.90, cancelled 1, total 5; each
  status filter returns the right count; a past range returns 0; `to` includes
  bills taken later that day; a bad date is 400
- Supply chain verified over real HTTP: PENDING leaves stock alone; receiving
  moves beef 2.5 → 22.5, refreshes its cost, writes an IN movement attributed to
  the receiver, and raises the payable 1240 → 1590; every guard returns the right
  status and a specific message
- Reports verified over real HTTP: revenue 16.50 / cost 6.52 / profit 9.98 /
  margin 60.48%; the 7-day series is zero-filled (7 points, 6 quiet days);
  category shares sum to **100.0%**; CSV downloads as
  `text/csv` with `Content-Disposition: attachment` and a UTF-8 BOM
- Settings verified over real HTTP: a cashier can read but not write; a
  non-numeric, negative, >100% VAT rate and a zero exchange rate are each
  rejected with a specific message; a partial update leaves other keys intact;
  setting VAT to 0 makes the next bill total its subtotal
- Profile verified over real HTTP: a cashier POSTing `role: ADMIN` and
  `username: admin` alongside their name still comes back **CASHIER / cashier**;
  taking another user's email is 409
- `npm run build` passes; `tsc --noEmit` reports 0 errors; **28 routes** build,
  all return 200 and an unknown route 404s
- `npm run lint` — **0 errors, 0 warnings**
- CORS preflight from `http://localhost:3000` returns
  `Access-Control-Allow-Credentials: true`, and the refresh cookie is accepted
  cross-origin (`HttpOnly; SameSite=Lax; Path=/api/auth`)
- Swagger UI and `/v3/api-docs` both return 200 (springdoc 2.8.9 — see the note below)

### Screens

| Route | Screen |
|---|---|
| `/login` | ចូលប្រើប្រាស់ · Login |
| `/signup` | បង្កើតគណនី · Sign up |
| `/forgot-password` | ភ្លេចពាក្យសម្ងាត់ · Request a reset code |
| `/verify-otp` | បញ្ជាក់លេខកូដ · 6-box OTP, paste-aware, 60s resend timer |
| `/reset-password` | កំណត់ពាក្យសម្ងាត់ថ្មី · Set a new password |

Behind the login, **28 routes** are wired. Built and working:

| Area | Screens |
|---|---|
| Cashier (teal chrome) | ✅ tables · payment · receipt · receipt/[id] · history |
| POS (navy, full-screen) | ✅ `/cashier/order` — no sidebar by design |
| Admin — master data | ✅ products · categories · tables · staff |
| Admin — supply chain | ✅ suppliers · purchase · stock |
| Admin — reporting | ✅ dashboard · reports (charts + CSV export) |
| Cashier | ✅ home · profile |
| Admin — settings | ✅ settings · change-password |
| Admin — dev aid | ✅ ui-kit component gallery |

**No placeholders remain** — every screen in the prototype is now a real screen.

Sign in as `admin` → lands on `/admin`; any other role → `/cashier/order`.
The sidebar highlights the current page, collapses to a drawer under 768px,
and the clocks tick live.

## ⚠️ What is *not* verified

Being straight about the gaps, because "138 tests pass" can read as more than it is.

**The suite has never run against real PostgreSQL.** All 138 tests execute on H2
in PostgreSQL-compatibility mode. Testcontainers is wired up:

```bash
cd backend && ./gradlew postgresTest
```

That task is proven to *reach* Docker — it fails with
`DockerClientProviderStrategy` on this machine, which has no Docker daemon — but
it has never been observed passing. **Run it once on a machine with Docker before
trusting the migrations on PostgreSQL.** H2 in PG mode is close, not identical.

Also not done:

- **No frontend tests.** The UI is verified by `tsc`, `eslint`, `next build` and
  manual HTTP checks — not by Playwright or Vitest.
- **No brute-force throttle beyond per-account lockout.** Five failed logins lock
  *that* account, but nothing rate-limits an attacker spraying one password across
  many usernames.
- **No file upload.** Product images are emoji strings.
- **No ingredient consumption on sale** — see open question 0 in the spec.
- **Khmer UI text is still my placeholder**, not the copy from your Figma file.

## Hardening (milestone 14)

Error responses now use the `ApiResponse` shape with an honest status:

| Request | Before | After |
|---|---|---|
| unknown path | 500 | **404** `No endpoint matches this path` |
| wrong method | 500 | **405** `DELETE is not supported here. Allowed: [GET]` |
| non-JSON body | 500 | **415** `Content-Type 'text/plain' is not supported…` |
| missing parameter | 500 | **400** `Required parameter 'tableId' is missing` |
| non-numeric path id | 500 | **400** |

An anonymous probe of an unknown path still gets **401**, so paths cannot be
enumerated without credentials. Stack traces and exception class names never
reach the client.

Security headers on every response: `Content-Security-Policy`,
`Referrer-Policy`, `Permissions-Policy`, `X-Content-Type-Options`,
`X-Frame-Options`, plus HSTS when served over HTTPS.

**springdoc was bumped 2.6.0 → 2.8.9.** On 2.6.0, `/v3/api-docs` returned 500
with `NoSuchMethodError: ControllerAdviceBean.<init>(Object)` — it is incompatible
with Spring Framework 6.2. It broke the moment a `@RestControllerAdvice` was
added in milestone 3 and went unnoticed until now.

### Trying the API

```bash
curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"ChangeMe123!\"}"
```

Or click **Authorize** in Swagger UI and paste the returned `accessToken`.

### Smoke-testing every endpoint

```bash
node scripts/smoke-api.mjs            # one line per call
node scripts/smoke-api.mjs --quiet    # only failures and the tally
```

Signs in, walks all 72 endpoints against a **running** backend, and prints what
each one answered. Exits `0` when everything matched, `1` on any mismatch, `2`
if it refused to run — so it can gate a deploy.

It covers the refusals as well as the successes: a cashier reaching an admin
route, a request with no token, and the delete guards on a category that still
has products, a stock item with movement history, and a supplier who is still
owed money. Those are the paths most likely to rot silently.

This does not replace `./gradlew test`. Those 166 tests reach branches this
cannot, roll back after themselves, and need no server. What this adds is the
one thing they cannot give: proof that the assembled, running application
answers correctly over real HTTP, through the security filter chain, against a
real database. Two bugs in this project's history lived exactly there — every
test green, the deployed answer still wrong.

⚠️ **It writes.** It creates categories, products, tables, staff, suppliers,
stock, purchase orders, a user and paid orders. Most are cleaned up; a received
purchase order and a paid order deliberately cannot be. Point it at a
disposable database — it refuses a non-localhost URL unless
`SMOKE_ALLOW_REMOTE=1`.

Overridable with `SMOKE_BASE_URL`, `SMOKE_ADMIN_USER`, `SMOKE_ADMIN_PASS`,
`SMOKE_CASHIER_USER`, `SMOKE_CASHIER_PASS`.

#### Clearing up after it

```bash
psql -U rms -d rms -h localhost -f scripts/clean-smoke-data.sql
```

Against H2 the leftovers vanish on restart; against PostgreSQL they stay. This
removes them and gives back the stock those smoke sales consumed. Safe to run
when there is nothing to clean, and safe to run twice.

It deletes only rows carrying the smoke test's markers — `user_id LIKE 'smoke%'`,
codes `LIKE 'SMOKE-%'`, names `LIKE 'Smoke %'`, and orders rung up by one of
those accounts. That last one is why the smoke test registers its own cashier
and rings the sales up as them: a smoke order is otherwise indistinguishable
from a real one, and the cleanup would have to delete every order and hope none
of them mattered. Change a marker in `smoke-api.mjs` and this script has to
change with it.

Password policy for register / reset / change: at least 8 characters, one
uppercase letter, one number. Five failed logins lock the account.

Password reset emails only send when `MAIL_USERNAME` / `MAIL_PASSWORD` are set;
otherwise the OTP is written to the application log so the flow stays testable.

### Default accounts

Created at first startup by `config/DataInitializer`, only if no user exists:

| Username | Password | Role |
|---|---|---|
| `admin` | `ChangeMe123!` | ADMIN |
| `cashier` | `ChangeMe123!` | CASHIER |

⚠️ Development credentials. Change both before this leaves localhost.


### Installed versions

| | Version |
|---|---|
| Spring Boot | 3.5.6 (Java 17, Gradle 8.14.3) |
| Next.js | 16.3.1 (React 19.2.8) |
| Tailwind CSS | v4 — **CSS-first config, no `tailwind.config.ts`** |

## Related folders

| Path | What it is |
|---|---|
| `../Prototype-Restaurant-mgs` | HTML prototype — **the UI reference**, 23 screens |
| `../Restaurant-Management-System-NIEI-Y4-` | Existing Spring Boot API — auth, staff, mail |

## ⚠️ Before copying config from the old repo

`Restaurant-Management-System-NIEI-Y4-/src/main/resources/application.properties`
has the database password, JWT signing secret, and a Gmail app password committed
in plain text. Those credentials are in that repo's git history and should be
rotated. Use `application-local.yml` (gitignored) or environment variables here.
