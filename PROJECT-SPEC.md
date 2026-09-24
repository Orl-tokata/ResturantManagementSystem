# Restaurant Management System — Build Specification

**Stack:** Spring Boot 3.5.6 (Java 17) REST API + Next.js (App Router) frontend
**Source of truth for UI:** `D:\Resturant_Management\Prototype-Restaurant-mgs`
**Source of truth for backend conventions:** `D:\Resturant_Management\Restaurant-Management-System-NIEI-Y4-`

This document is the build plan. Work through §10 (Build Order) top to bottom.

---

## 1. Goal

Rebuild the clickable HTML prototype as a real application:

- **Backend** — Spring Boot REST API, JWT auth, PostgreSQL, Swagger.
  Carries over the existing package structure, `ApiResponse<T>` envelope,
  and `UserInfm` auth model from the NIEI-Y4 repo.
- **Frontend** — Next.js App Router, TypeScript, Tailwind CSS.
  Every screen in the prototype becomes a route; the prototype's CSS tokens
  become the Tailwind theme.

The prototype's **layout and palette are authoritative**. Its Khmer label text is
placeholder (see prototype `README.md`) — replace with real copy from Figma as it
becomes available.

---

## 2. Repository layout

```
Restaurant-Management-System-Spring-Boot/
├── PROJECT-SPEC.md              ← this file
├── README.md                    quick start
├── docker-compose.yml           postgres (+ pgadmin) for local dev
│
├── backend/                     Spring Boot
│   ├── build.gradle
│   ├── src/main/java/com/resturant/management/rms/
│   │   ├── RmsApplication.java
│   │   ├── config/              Cors, Jwt, Security, Swagger
│   │   ├── security/            JwtAuthenticationFilter
│   │   ├── common/              ApiResponse, PageResponse, exceptions
│   │   ├── auth/                controller · service · dto
│   │   ├── user/                UserInfm, Role, repository, service
│   │   ├── staff/
│   │   ├── catalog/             Category, Product
│   │   ├── dining/              DiningTable
│   │   ├── order/               Order, OrderItem
│   │   ├── supplier/
│   │   ├── purchase/            Purchase, PurchaseItem
│   │   ├── stock/               StockItem, StockMovement
│   │   ├── report/
│   │   └── setting/
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml       (gitignored)
│       └── db/migration/              Flyway V1__init.sql, V2__seed.sql
│
└── frontend/                    Next.js
    ├── package.json
    ├── tailwind.config.ts
    ├── src/
    │   ├── app/
    │   │   ├── (auth)/          login, signup, forgot-password, verify-otp, reset-password
    │   │   ├── (cashier)/       cashier/*
    │   │   ├── (admin)/         admin/*
    │   │   └── layout.tsx
    │   ├── components/          ui/ · layout/ · pos/
    │   ├── lib/                 api client, auth, formatters
    │   ├── hooks/
    │   └── types/
    └── public/
```

**Package rename note:** the old repo uses
`com.resturant.management.ResturantManagementSystem` (a class-cased segment, and
"Resturant" is misspelled). This spec uses `com.resturant.management.rms` —
shorter and lowercase-correct. Keep the group id `com.resturant.management` so
Gradle coordinates stay familiar.

---

## 3. Screen → route map

Every prototype file maps to exactly one Next.js route.

### Auth — `src/app/(auth)/`

| Prototype file | Route | Notes |
|---|---|---|
| `login.html` | `/login` | role select drives post-login redirect |
| `signup.html` | `/signup` | |
| `forgot-password.html` | `/forgot-password` | posts email, sends OTP |
| `otp-verify.html` | `/verify-otp` | 6-digit input, 60s resend timer |
| `reset-password.html` | `/reset-password` | consumes reset token |

### Cashier — `src/app/(cashier)/`

| Prototype file | Route | Notes |
|---|---|---|
| `cashier-home.html` | `/cashier` | KPI tiles + recent orders |
| `cashier-order.html` | `/cashier/order` | **POS** — full-screen, no sidebar |
| `cashier-tables.html` | `/cashier/tables` | table picker, colour-coded status |
| `cashier-payment.html` | `/cashier/payment` | keypad, method select, change calc |
| `cashier-receipt.html` | `/cashier/receipt/[id]` | print stylesheet |
| `cashier-history.html` | `/cashier/history` | date range + status filter |
| `cashier-profile.html` | `/cashier/profile` | profile + change password |

### Admin — `src/app/(admin)/`

| Prototype file | Route | Notes |
|---|---|---|
| `admin-dashboard.html` | `/admin` | charts, best sellers, low-stock |
| `admin-products.html` | `/admin/products` | list + add/edit modal + delete confirm |
| `admin-categories.html` | `/admin/categories` | same pattern |
| `admin-tables.html` | `/admin/tables` | same pattern |
| `admin-staff.html` | `/admin/staff` | same pattern |
| `admin-suppliers.html` | `/admin/suppliers` | same pattern |
| `admin-purchase.html` | `/admin/purchase` | PO with line items |
| `admin-stock.html` | `/admin/stock` | level meters, adjustment modal |
| `admin-reports.html` | `/admin/reports` | tabs, date range, export |
| `admin-settings.html` | `/admin/settings` | restaurant info, sales, toggles |
| `admin-change-password.html` | `/admin/change-password` | success modal |

`index.html` (the prototype launcher) has no equivalent — it was a dev aid.

---

## 4. Domain model

### 4.1 Carried over from the existing repo

| Entity | Table | Change needed |
|---|---|---|
| `UserInfm` | `USERS_INFM` | keep as-is (audit columns, lockout logic, `UserDetails`) |
| `PasswordResetToken` | — | keep |
| `Staff` | `STAFF_INFM` | **extend** — see below |
| `Role` enum | — | **extend** — see below |

`Role` currently has `USER, ADMIN`. The prototype needs four:

```java
public enum Role { ADMIN, CASHIER, WAITER, CHEF }
```

Migrate existing rows: `USER → CASHIER`.

`Staff` currently has only `staffID, sName, sPhone, sRole`. The
`admin-staff.html` modal requires: code, gender, date of birth, email, shift,
salary, hire date, address, status.

### 4.2 New entities

```
Category      id, name, nameEn, icon, status, + audit
Product       id, name, nameEn, categoryId→Category, price, cost,
              stockQty, imageUrl, description, status, + audit

DiningTable   id, name, seats, zone, status, + audit
              zone   ∈ INDOOR | OUTDOOR | VIP
              status ∈ FREE | OCCUPIED | RESERVED

Order         id, invoiceNo, tableId→DiningTable, cashierId→UserInfm,
              subtotal, discount, vatRate, vatAmount, total, totalKhr,
              paymentMethod, status, paidAt, + audit
              paymentMethod ∈ CASH | CARD | KHQR | TRANSFER
              status        ∈ OPEN | PAID | CANCELLED
OrderItem     id, orderId→Order, productId→Product, productName,
              qty, unitPrice, lineTotal

Supplier      id, code, company, contactPerson, phone, email,
              supplyType, address, balance, status, + audit
Purchase      id, poNo, supplierId→Supplier, purchaseDate, total, status, + audit
              status ∈ PENDING | RECEIVED | CANCELLED
PurchaseItem  id, purchaseId→Purchase, stockItemId→StockItem, qty, unitCost, lineTotal

StockItem     id, name, unit, qty, minQty, unitCost, + audit
StockMovement id, stockItemId→StockItem, type, qty, reason, createdBy, createdAt
              type ∈ IN | OUT | DAMAGED

AppSetting    id, settingKey (unique), settingValue, description
```

**Audit columns** — follow the existing `UserInfm` convention:
`regId, regDtm, modId, modDtm, actYn`. Put them in a `@MappedSuperclass`
called `BaseAuditEntity` rather than repeating them 12 times.

`productName` is denormalised onto `OrderItem` on purpose: a receipt must
still print correctly after a product is renamed or deleted.

**Money:** `BigDecimal(12,2)` everywhere. Never `double`. `totalKhr` is
`BigDecimal(14,0)` — riel has no minor unit.

### 4.3 Relationship summary

```
UserInfm 1──* Order            (cashier)
DiningTable 1──* Order
Order 1──* OrderItem *──1 Product *──1 Category
Supplier 1──* Purchase 1──* PurchaseItem *──1 StockItem
StockItem 1──* StockMovement
```

---

## 5. REST API

Base path `/api`. Every response uses the existing `ApiResponse<T>` envelope:

```json
{ "status": 200, "message": "OK", "data": { }, "timestamp": "2026-08-16T12:44:00" }
```

List endpoints return `ApiResponse<PageResponse<T>>` with
`content, page, size, totalElements, totalPages`.

### Auth — `/api/auth` ✅ implemented

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/register` | public | 409 on duplicate username or email |
| POST | `/login` | public | access token in body, refresh token in an httpOnly cookie |
| POST | `/refresh` | cookie | reads `rms_refresh`, returns a new access token |
| POST | `/logout` | public | expires the cookie |
| POST | `/forgot-password` | public | emails a 6-digit OTP, valid 10 min |
| POST | `/verify-otp` | public | spends the OTP, returns a reset token valid 15 min |
| POST | `/reset-password` | public | consumes the reset token |
| POST | `/change-password` | **required** | verifies the current password first |
| GET | `/me` | **required** | current user for the sidebar |

> `/api/auth/**` is **not** blanket-permitted. The public paths are listed
> individually in `SecurityConfig.PUBLIC_PATHS`, because a wildcard would also
> expose `/me` and `/change-password` — which need a principal, and NPE'd into a
> 500 when they did not have one. Anything new under `/api/auth` is private
> unless explicitly added.

Two token types share one signing key but carry a different `typ` claim, so a
refresh token cannot be replayed as an access token. There is a test for that.

### CRUD resources

Identical shape for each: `GET /` (paged, `?search=&page=&size=`),
`GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`.

| Resource | Path | Read | Write | Status |
|---|---|---|---|---|
| Categories | `/api/categories` | any auth | ADMIN | ✅ |
| Products | `/api/products` | any auth | ADMIN | ✅ |
| Tables | `/api/tables` | any auth | ADMIN | ✅ |
| Staff | `/api/staff` | **ADMIN** | ADMIN | ✅ |
| Suppliers | `/api/suppliers` | ADMIN | ADMIN |
| Purchases | `/api/purchases` | ADMIN | ADMIN |
| Stock items | `/api/stock` | ADMIN | ADMIN |
| Settings | `/api/settings` | any auth | ADMIN |

### Non-CRUD endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/products?categoryId=` | POS category filter |
| PATCH | `/api/tables/{id}/status` | free ⇄ occupied ⇄ reserved |
| POST | `/api/orders` | ✅ open a bill — **returns the one already open at that table** rather than erroring, so a double-tap or a second till cannot create competing bills |
| GET | `/api/orders/open?tableId=` | ✅ recover the open bill when the POS reloads (404 if none) |
| PUT | `/api/orders/{id}/items` | ✅ replace the whole line-item set |
| POST | `/api/orders/{id}/cancel` | ✅ cancel and free the table |
| POST | `/api/orders/{id}/pay` | method + tendered → total, change, marks PAID |
| POST | `/api/orders/{id}/cancel` | |
| GET | `/api/orders/{id}/receipt` | receipt projection |
| GET | `/api/orders?search=&status=&from=&to=` | ✅ history, paged. Dates are `LocalDate` and cover **whole days** — an exclusive `to` at midnight would drop every bill taken on the last day of the range |
| GET | `/api/orders/summary?from=&to=` | ✅ the four history tiles: paid total, paid count, average, cancelled count |
| POST | `/api/stock/{id}/adjust` | ✅ type + qty + reason → writes a StockMovement. `qty` is **always positive**; the direction comes from the type, so a caller cannot accidentally add by sending a negative to an OUT |
| GET | `/api/stock/{id}/movements` | ✅ audit trail for one item |
| GET | `/api/stock/summary` · `/api/stock/low` | ✅ tiles and reorder list |
| POST | `/api/purchases/{id}/receive` | ✅ one transaction: increase each line's stock, write an IN movement per line, refresh unit costs, add the total to the supplier's payable |
| POST | `/api/purchases/{id}/cancel` | ✅ pending only — 400 once received, since cancelling then would overstate stock |
| GET | `/api/purchases/summary` | ✅ month total, order count, pending count, total payable |
| GET | `/api/reports/sales?from=&to=` | dashboard + reports |
| GET | `/api/reports/best-sellers?limit=` | |
| GET | `/api/reports/low-stock` | |
| GET | `/api/dashboard/summary` | the four KPI tiles |

### Delete guards (milestone 7)

Deletes are checked in the service and answered with 409 and a specific message,
rather than letting a foreign-key violation surface as a 500:

- **Category** with products → `Cannot delete Category 'បាយ': it is still used by N product(s)`
- **Table** that is occupied → `Cannot delete table 'Table 02' while it is occupied`
- **Product** deletes freely — `OrderItem` keeps a nullable `product_id` plus its
  own copy of name and price, so historical receipts stay correct.

`GlobalExceptionHandler` also maps `DataIntegrityViolationException` to a 409 as
a backstop, and enum type-mismatches to 400 rather than 500.

### Money arithmetic (milestone 8)

`OrderService.recalculate` is the single place totals are computed:

```
subtotal = Σ (qty × unitPrice)          scale 2, HALF_UP
taxable  = subtotal − discount          rejected if discount > subtotal
vat      = taxable × vatRate / 100      scale 2, HALF_UP
total    = taxable + vat
totalKhr = total × khrRate              scale 0 — riel has no minor unit
```

`vatRate` and `khrRate` come from `AppSetting` via `SettingService`, which falls
back to 10% and 4100 if a row is missing or unparseable — a bad settings row must
not stop the tills.

The rate is copied onto the order when it is opened, so changing VAT later does
not silently restate bills that are already open.

### Transactional rules

- ✅ `POST /api/orders/{id}/pay` is `@Transactional`: mark PAID → record tender
  and change → decrement `Product.stockQty` → set table FREE. All or nothing;
  a rejected tender leaves the bill OPEN, stock untouched and the table occupied.

  **Deviation from the original plan:** no `StockMovement` rows are written for
  sales. Movements are keyed to `stock_item` (raw ingredients), and nothing maps
  a dish to its ingredients — see the recipe/BOM question in §12. Movements are
  written by the purchase and adjustment flows in milestone 11, which do operate
  on stock items.

  Stock is allowed to go negative rather than blocking the sale: by the time the
  bill is settled the food has already left the kitchen, so refusing payment
  would be the wrong answer. A negative figure is a signal for the stock screen,
  and it is logged as a warning.
- `invoiceNo` and `poNo` come from a DB sequence, formatted `INV-%05d` /
  `PO-%05d`. Do **not** generate them with `count()+1` — that races.
- `POST /api/purchases` with status RECEIVED increments `StockItem.qty`.

---

## 6. Security

Carry over `JwtAuthenticationFilter`, `JwtService`, `CustomUserDetailsService`.

- Access token 1h, refresh token 24h (matches existing config).
- `SecurityConfig`: permit `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`;
  everything else authenticated. Method-level `@PreAuthorize("hasRole('ADMIN')")`
  on admin-only writes.
- CORS: allow `http://localhost:3000` in dev — set via config property, not hardcoded.
- Frontend stores the access token in memory + refresh token in an
  **httpOnly cookie**. Do not put JWTs in `localStorage`.

> ⚠️ **The existing repo has live secrets committed to git** —
> `application.properties` contains the real PostgreSQL password, the JWT
> signing secret, and a Gmail app password. Do not copy that pattern here.
> Use `application-local.yml` (gitignored) or environment variables, and rotate
> the Gmail app password and JWT secret, since they are in the NIEI-Y4 git history.

---

## 7. Frontend architecture

### 7.1 Setup

Already scaffolded (milestone 1) with:

```bash
npx create-next-app@latest frontend --typescript --tailwind --eslint --app --src-dir --import-alias "@/*"
```

**Installed versions** — note these differ from what an older tutorial assumes:

| | Version | Consequence |
|---|---|---|
| Next.js | **16.3.1** | App Router, Turbopack build by default |
| React | **19.2.8** | |
| Tailwind CSS | **v4** | **CSS-first config — there is no `tailwind.config.ts`** (see §7.2) |
| TypeScript | 5.x | |

> Node 20.14.0 is installed. One transitive ESLint dependency asks for
> `^20.19 || ^22.13 || >=24` and prints an `EBADENGINE` warning. Install and
> build both succeed, but upgrading to Node 22 LTS removes the noise.

Additional dependencies (installed):

| Package | Why |
|---|---|
| `@tanstack/react-query` | server state, caching, refetch |
| `axios` | API client with interceptors for JWT refresh |
| `react-hook-form` + `zod` | forms and validation |
| `recharts` | dashboard and report charts |
| `lucide-react` | icons (replaces the prototype's emoji) |
| `date-fns` | date formatting |

### 7.2 Design tokens → Tailwind v4

Tailwind v4 dropped the JS config file. The theme is declared **in CSS** with
`@theme`, already done in `frontend/src/app/globals.css`:

```css
@import "tailwindcss";

@theme {
  --color-teal-800: #245953;   /* → bg-teal-800, text-teal-800, border-teal-800 */
  --color-brand-600: #1b9c85;
  --color-navy-800: #323759;
  --color-orange-500: #ff8b13;
  /* … full palette in the file … */
}
```

Every `--color-*` key automatically generates the matching utilities. Palette
values came from `Prototype-Restaurant-mgs/assets/css/tokens.css`, which was
sampled from the Figma canvas — they are the real design colours.

Namespace map, prototype → Tailwind:

| Prototype token | Tailwind utility |
|---|---|
| `--teal-800` auth/cashier chrome | `teal-800` |
| `--green-600` admin chrome | `brand-600` |
| `--navy-800` POS chrome | `navy-800` |
| `--orange-500` POS actions | `orange-500` |
| `--grey-*` neutrals | `ink-*` |

`grey` was renamed `ink` to avoid colliding with Tailwind's built-in `gray`.

**Khmer typography:** handled in `layout.tsx` via `next/font/google` →
`Noto_Sans_Khmer`, self-hosted at build time and exposed as `--font-khmer`.
The prototype relied on the user having *Khmer OS* installed; production does not.

### 7.3 Component inventory

Derived from the prototype CSS classes — build these once in `components/ui/`:

All built in `components/ui/`, exported from `components/ui/index.ts`, and
rendered together at **`/admin/ui-kit`** — a development gallery, deliberately
not linked from the sidebar.

| Component | Prototype class | Used by |
|---|---|---|
| `<Button>` | `.btn` + variants | everywhere |
| `<Field> <Input> <Select> <Textarea> <Checkbox> <FieldRow>` | `.field .input .select` | all forms |
| `<SearchBar>` | `.searchbar` | every list screen — 300ms debounce |
| `<Card> <Toolbar> <PageTitle>` | `.card` `.toolbar` | everywhere |
| `<DataTable>` | `.table` `.table-wrap` | 9 list screens — generic over row type |
| `<Pagination>` | prototype pager | list screens — zero-based, matches Spring Data |
| `<Badge>` | `.badge--ok/warn/dead/info` | status columns |
| `<StatTile> <StatGrid>` | `.stat` | dashboards |
| `<Modal>` | `.modal-backdrop` `.modal` | every add/edit |
| `<ConfirmDialog>` | delete modal pattern | 6 screens |
| `<Tabs>` | `.tabs` | reports |
| `<Meter>` | `.meter` | stock levels |
| `<EmptyState>` | — | added: lists need a zero-row state the prototype never showed |
| `<Alert>` | — | added: form and request errors |

**Light by default.** Controls are styled for white surfaces, since that is most
of the app. The auth card sits on teal, so `AuthCard` marks its subtree
`.auth-surface` and a short block in `globals.css` re-colours labels, inputs and
ghost buttons by cascade — rather than every field taking a `tone` prop.

**Status colours are decided once.** `toneForOrderStatus`, `toneForTableStatus`
and `toneForRecordStatus` live next to `<Badge>`, so PAID is never green on one
screen and grey on another.

`<Modal>` traps Tab, closes on Escape, restores focus to whatever opened it, and
locks body scroll. It closes on backdrop `mousedown`, not `click`, so a text
selection that ends outside the panel does not dismiss it.

`components/layout/`: `<Sidebar>` `<Topbar>` `<StatusBar>` `<AppShell>` —
mirrors `renderShell()` in the prototype's `proto.js`, including the
`MENUS` definition (admin vs cashier menus).

`components/pos/`: `<CategoryRail>` `<ProductGrid>` `<ProductCard>`
`<OrderPanel>` `<TotalsBox>` `<Keypad>` `<TableCard>`.

### 7.4 Layout groups

Built as:

```
app/
  (auth)/            centred card on teal #245953, no chrome
    login · signup · forgot-password · verify-otp · reset-password
  (protected)/       layout = <RequireAuth>
    admin/
      layout.tsx     <AppShell variant="admin">   green chrome
      … 11 pages
    cashier/
      (shell)/
        layout.tsx   <AppShell variant="cashier"> teal chrome
        … 6 pages
      (pos)/
        order/       NO shell — full viewport, navy chrome
```

**Why two route groups under `cashier/`:** `/cashier/order` must not inherit the
sidebar, but it is a sibling URL of the screens that do. Route groups add no URL
segment, so `(shell)` and `(pos)` give the two halves different layouts while
keeping `/cashier/tables` and `/cashier/order` as siblings. Toggling the shell
with a `usePathname()` check inside one layout would work too, but it makes the
POS pay for chrome it then hides.

### 7.5 Session handling

The access token is held **in a module variable in `lib/api.ts`**, never in
`localStorage` — an XSS payload can read storage but not a closure. Durability
comes from the httpOnly refresh cookie, which JavaScript cannot touch at all.
`AuthProvider` attempts a silent `POST /auth/refresh` on mount, so a page reload
keeps the user signed in.

Route protection is **client-side on purpose**. Next middleware runs before the
app has a token in memory and cannot read the `/api/auth`-scoped httpOnly
cookie, so it could not make a correct decision. `RequireAuth` only avoids
rendering a shell whose data calls would 401 — the backend remains the actual
enforcement point.

`?next=` on the login URL is honoured only when it starts with a single `/`,
otherwise the login screen would be an open redirect.

---

## 8. Database

> **This machine has no Docker installed, and a local PostgreSQL 15 service is
> already running on port 5432.** So `docker-compose.yml` publishes the container
> on **5433** to avoid the clash. Until a database is provisioned, use the `dev`
> profile (in-memory H2, Flyway off) — see §9.

PostgreSQL 16 via Docker:

```yaml
# docker-compose.yml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: rms
      POSTGRES_USER: rms
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
volumes: { pgdata: }
```

Use **Flyway**, not `ddl-auto=update`. The old repo uses `update`, which silently
skips destructive changes and leaves dev and prod schemas drifting apart.

Migrations as built:

| File | Contents |
|---|---|
| `V1__baseline.sql` | `seq_invoice_no`, `seq_purchase_no` |
| `V2__init.sql` | 14 tables, FKs, CHECK constraints, 11 indexes |
| `V3__seed.sql` | 8 categories, 16 products, 12 tables, 7 staff, 5 suppliers, 10 stock items, 15 settings |

`spring.jpa.hibernate.ddl-auto=validate` in every profile.

User accounts are **not** seeded in SQL — passwords must be BCrypt-hashed by the
application, and a hash committed to a migration would be a shared public
credential. `config/DataInitializer` creates `admin` and `cashier` idempotently
at startup instead.

Explicit indexes: `orders(status)`, `orders(reg_dtm)`, `orders(table_id)`,
`order_item(order_id)`, `product(category_id)`, `purchase(supplier_id)`,
`purchase(purchase_date)`, `purchase_item(purchase_id)`,
`stock_movement(stock_item_id)`, `stock_movement(created_at)`,
`password_reset_token(user_ref)`.

---

## 9. Local setup

### Fastest path — no database needed

```bash
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'
```

In-memory H2 in **PostgreSQL compatibility mode**, with Flyway enabled and
Hibernate set to `validate` — the same migrations, the same validation as
production. The migrations are written in portable DDL precisely so this works,
which means a mismatch between a migration and an entity mapping fails at
startup instead of on deploy.

The only difference from production is that the database is discarded on
shutdown. Suitable for feature work; just re-seed by restarting.

### Real path — PostgreSQL

Either start the container:

```bash
cp .env.example .env        # then set DB_PASSWORD
docker compose up -d db     # publishes on host port 5433
```

…or use the PostgreSQL 15 already installed on this machine:

```sql
CREATE USER rms WITH PASSWORD 'your-password';
CREATE DATABASE rms OWNER rms;
```

Then either copy `backend/src/main/resources/application-local.yml.example` to
`application-local.yml` and fill it in, or export `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `JWT_SECRET`.

### Frontend

```bash
cd frontend && npm run dev
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API health | http://localhost:8081/api/health |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| H2 console (`dev` only) | http://localhost:8081/h2-console |

Backend port stays **8081** to match the existing repo, leaving 3000 free for Next.

---

## 10. Build order

Each milestone should end in a runnable state.

| # | Milestone | Deliverable |
|---|---|---|
| 1 | ✅ **Scaffold** | Gradle + Next projects, docker-compose, health check green |
| 2 | ✅ **Schema** | Flyway V1–V3, 14 entities, 12 repositories, 6 passing tests |
| 3 | ✅ **Auth backend** | register/login/refresh/me + JWT filter + roles, 22 passing tests |
| 4 | ✅ **Auth frontend** | 5 auth screens, axios interceptor, protected routes |
| 5 | ✅ **App shell** | Sidebar/Topbar/StatusBar, both menus, 24 routes wired |
| 6 | ✅ **UI kit** | the components in §7.3 + gallery at `/admin/ui-kit` |
| 7 | ✅ **Master data** | categories, products, tables, staff — CRUD both ends, 38 tests |
| 8 | ✅ **POS** | order screen, table picker, cart state, open order — 52 tests |
| 9 | ✅ **Payment** | payment screen, `/pay` transaction, receipt + print, 65 tests |
| 10 | ✅ **History** | order history with filters, summary tiles, 77 tests |
| 11 | ✅ **Supply chain** | suppliers, purchases, stock, adjustments, 97 tests |
| 12 | ✅ **Reports** | dashboards, charts, sales report, CSV export, 109 tests |
| 13 | ✅ **Settings** | app settings, self-service profile, change password, 122 tests |
| 14 | ✅ **Hardening** | error surface, security headers, Testcontainers wiring |
| 15 | ✅ **i18n** | next-intl, km/en switcher, localized API errors — 153 tests |

Milestones 1–5 are sequential. 7–13 are independent once 6 lands.

All fifteen are done. What is left is in §12, which is decisions rather than
build order — and one known gap, recorded there.

---

## 11. Conventions

**Backend**
- Controllers stay thin — no business logic, no repository calls.
- Entities never cross the HTTP boundary; map to DTOs.
- `@RestControllerAdvice` for a single error shape via `ApiResponse`.
- Bean Validation (`@Valid`, `@NotBlank`, `@Positive`) on every request DTO.
- Constructor injection, not `@Autowired` fields.
- Delete `spring.main.allow-circular-references=true` (present in the old repo) —
  it hides a design problem rather than fixing it.

**Frontend**
- Server Components by default; `"use client"` only for interactivity.
- All API calls through `lib/api.ts`; no bare `fetch` in components.
- React Query for server state; local state stays local. No Redux.
- Money formatted through one helper (`formatUsd`, `formatKhr`) — never inline.

**Both**
- Text comes from the message catalogues, never from a literal in a component:
  `frontend/messages/{km,en}.json` and `backend/.../messages/messages_{km,en}.properties`.
  The prototype's `ខ្មែរ · English` labels were a placeholder for this and are gone.
- A key is a string, so nothing checks it at compile time. Both sides have a
  guard instead: `MessageBundleTest` on the backend, and the audit script on
  the frontend that resolves every `t()` and `apiError()` call back to a key.
- Currency: USD primary, KHR secondary at a configurable rate (default 4100).

---

## 12. Decisions and what is left

### Settled

- **Reuse or rewrite the NIEI-Y4 backend.** Rewritten as a fresh module with the
  conventions carried over: the `ApiResponse` envelope, the `USERS_INFM` column
  names (`user_id`, `user_pwd`, `reg_id`/`reg_dtm`), `UserInfm.bizKey` as a
  10-character NOT NULL UNIQUE column, and the `Role` enum. That reference repo
  stopped at its 2025-10-13 commit and everything in it now has a counterpart
  here, so it is a historical reference, not a source to sync from. Its two
  utilities were deliberately not carried over: `GenerateKey` printed a JWT
  secret from a `main()`, which this app replaces with a required `JWT_SECRET`
  and a fail-fast check at startup, and `DateTimeUtil`'s formatters belong on
  the frontend, where the locale is known.

- **i18n.** next-intl with a km/en switcher, not the prototype's hardcoded
  `ខ្មែរ · English` labels. The API localizes its own errors from
  `Accept-Language`, because a message the server refuses with cannot be
  translated on the client.

- **Recipe / bill of materials.** Out of scope, decided by shipping milestone 11
  without it. Paying decrements the dish's own `stockQty` and writes no
  `StockMovement`, so ingredient stock is tracked only by explicit purchases and
  adjustments. Revisit with a `product_ingredient` table and a movement-on-sale
  step if that stops being good enough.

- **Rate limiting.** Done. Account lockout only ever sees one account, which
  misses credential stuffing across many and lets anyone lock out a username
  they know; a cap per caller covers both, and is the only thing that covers
  guessing a six-digit OTP. Token bucket, in memory, at the front of the
  security chain so a flood is refused before it costs a password hash:
  login 10/min, forgot-password 5/10min, verify-otp 10/10min, register 5/hour.
  `X-Forwarded-For` is ignored unless `trust-forwarded-for` is set, because
  honouring a caller-supplied header with no proxy in front means no limit at
  all. Off on the dev profile, which the suite runs on; `RATE_LIMIT_ENABLED=true`
  turns it on locally.

  In memory means per instance: a second instance behind a load balancer would
  allow the limit again, and `RateLimiter.tryConsume` is where a shared counter
  would go.

### Still open

1. **Khmer copy.** Mostly answered, and the answer was that there is very
   little of it. The Figma file holds 64 distinct Khmer strings against 460
   Latin ones — an English-first design with a Khmer login and auth flow and a
   partly Khmer POS screen. Strip the sample dish names and font specimens and
   about 37 strings are real UI copy, all of which are now in the catalogue;
   eight already matched. The remaining ~450 Khmer strings have no Figma source
   and are ours, so they are the thing to have a Khmer speaker review.

   Read the file with a `use_figma` script rather than `get_metadata`: the page
   metadata is 221KB and will not cross the MCP transport, while a script
   returns only what it selects.

   Two things in the design were deliberately not adopted — it spells invoice
   វិក័យប័ត្រ once where the catalogue uses the standard វិក្កយបត្រ in 39 places,
   and its forgot-password screen shows the password after a name is typed,
   which is neither the built flow nor free of typos (លេងសម្ងាត់ should be
   លេខសម្ងាត់). The file has Latin typos too — Cagegory, Veriry, Passowrd,
   Criscital — so it is a source for wording, not for spelling.
2. **Printing.** Browser print today, as prototyped. A thermal ESC/POS printer
   would change the receipt implementation substantially.
3. **Multi-tenancy.** `bizKey` exists on every user and is generated per
   account, but nothing scopes a query by it — one restaurant per deployment.
   Real tenanting means a tenant table, `bizKey` on every business row, and a
   filter applied to every query; it is much easier to add before there is data
   than after.
