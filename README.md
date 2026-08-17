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

**Milestones 1–13 complete and verified — every feature milestone is done.**
Scaffold · Schema · Auth backend · Auth frontend · App shell · UI kit ·
Master data · POS · Payment · History · Supply chain · Reports · Settings

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
- `./gradlew test` — **122 tests, 0 failures, 0 skipped**
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
- Swagger UI and `/v3/api-docs` both return 200

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

Next: **milestone 14 (Hardening)** — the last one: broader validation, a
consistent error surface, more tests, and moving the test suite onto
Testcontainers so it stops depending on H2. See
[PROJECT-SPEC.md](PROJECT-SPEC.md) §10.

### Trying the API

```bash
curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"ChangeMe123!\"}"
```

Or click **Authorize** in Swagger UI and paste the returned `accessToken`.

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
