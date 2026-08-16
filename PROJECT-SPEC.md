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

### Auth — `/api/auth` (public)

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/register` | RegisterRequest | exists |
| POST | `/login` | LoginRequest | returns access + refresh token, role |
| POST | `/refresh` | refreshToken | **new** |
| POST | `/logout` | — | exists |
| POST | `/forgot-password` | email | exists — sends OTP |
| POST | `/verify-otp` | email, code | **new** — returns reset token |
| POST | `/reset-password` | token, newPassword | exists |
| POST | `/change-password` | current, new | authenticated |
| GET | `/me` | — | **new** — current user for the sidebar |

### CRUD resources

Identical shape for each: `GET /` (paged, `?search=&page=&size=`),
`GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`.

| Resource | Path | Read | Write |
|---|---|---|---|
| Categories | `/api/categories` | any auth | ADMIN |
| Products | `/api/products` | any auth | ADMIN |
| Tables | `/api/tables` | any auth | ADMIN |
| Staff | `/api/staff` | ADMIN | ADMIN |
| Suppliers | `/api/suppliers` | ADMIN | ADMIN |
| Purchases | `/api/purchases` | ADMIN | ADMIN |
| Stock items | `/api/stock` | ADMIN | ADMIN |
| Settings | `/api/settings` | any auth | ADMIN |

### Non-CRUD endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/products?categoryId=` | POS category filter |
| PATCH | `/api/tables/{id}/status` | free ⇄ occupied ⇄ reserved |
| POST | `/api/orders` | open an order for a table |
| PUT | `/api/orders/{id}/items` | replace the whole line-item set |
| POST | `/api/orders/{id}/pay` | method + tendered → total, change, marks PAID |
| POST | `/api/orders/{id}/cancel` | |
| GET | `/api/orders/{id}/receipt` | receipt projection |
| GET | `/api/orders?from=&to=&status=` | history screen |
| POST | `/api/stock/{id}/adjust` | type + qty + reason → writes StockMovement |
| GET | `/api/reports/sales?from=&to=` | dashboard + reports |
| GET | `/api/reports/best-sellers?limit=` | |
| GET | `/api/reports/low-stock` | |
| GET | `/api/dashboard/summary` | the four KPI tiles |

### Transactional rules

- `POST /api/orders/{id}/pay` must be `@Transactional`: mark PAID → decrement
  `Product.stockQty` → write `StockMovement` rows → set table FREE.
  All or nothing.
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

```bash
npx create-next-app@latest frontend --typescript --tailwind --eslint --app --src-dir
```

Additional dependencies:

| Package | Why |
|---|---|
| `@tanstack/react-query` | server state, caching, refetch |
| `axios` | API client with interceptors for JWT refresh |
| `react-hook-form` + `zod` | forms and validation |
| `recharts` | dashboard and report charts |
| `lucide-react` | icons (replaces the prototype's emoji) |
| `date-fns` | date formatting |

### 7.2 Design tokens → Tailwind

Copy the palette from `Prototype-Restaurant-mgs/assets/css/tokens.css` — these
values were sampled from the Figma canvas, so they are the real design colours.

```ts
// tailwind.config.ts
theme: {
  extend: {
    colors: {
      teal:   { 900:'#1b423d', 800:'#245953', 700:'#2e4f4f',
                600:'#0e8388', 500:'#14a2a8', 100:'#cbe4de' },
      brand:  { 700:'#158069', 600:'#1b9c85', 500:'#47a992', 200:'#b7e5dd' },
      sand:   { 300:'#eed180' },
      navy:   { 800:'#323759', 700:'#3d4368' },
      orange: { 500:'#ff8b13', 600:'#e07a0c' },
      cream:  { 100:'#fdf6e3' },
      danger: { DEFAULT:'#a81616', soft:'#e5484d' },
    },
    fontFamily: {
      ui: ['"Khmer OS Battambang"','"Noto Sans Khmer"','Hanuman','system-ui','sans-serif'],
    },
  },
}
```

**Khmer typography:** self-host Noto Sans Khmer via `next/font/local` rather than
relying on the user having *Khmer OS* installed. The prototype falls back to
system fonts; production must not.

### 7.3 Component inventory

Derived from the prototype CSS classes — build these once in `components/ui/`:

| Component | Prototype class | Used by |
|---|---|---|
| `<Button>` | `.btn` + variants | everywhere |
| `<Input> <Select> <Textarea>` | `.input .select .textarea` | all forms |
| `<SearchBar>` | `.searchbar` | every list screen |
| `<Card>` | `.card` `.card__head` | everywhere |
| `<DataTable>` | `.table` `.table-wrap` | 9 list screens |
| `<Badge>` | `.badge--ok/warn/dead/info` | status columns |
| `<StatTile>` | `.stat` | dashboards |
| `<Modal>` | `.modal-backdrop` `.modal` | every add/edit/delete |
| `<ConfirmDialog>` | delete modal pattern | 6 screens |
| `<Tabs>` | `.tabs` | reports |
| `<Meter>` | `.meter` | stock levels |

`components/layout/`: `<Sidebar>` `<Topbar>` `<StatusBar>` `<AppShell>` —
mirrors `renderShell()` in the prototype's `proto.js`, including the
`MENUS` definition (admin vs cashier menus).

`components/pos/`: `<CategoryRail>` `<ProductGrid>` `<ProductCard>`
`<OrderPanel>` `<TotalsBox>` `<Keypad>` `<TableCard>`.

### 7.4 Layout groups

- `(auth)` — centred card on teal `#245953`, no chrome
- `(cashier)` and `(admin)` — `<AppShell>` with sidebar + topbar + status bar
- **`/cashier/order` is the exception** — full-screen POS with navy chrome and
  no sidebar. Give it its own layout, not the shared shell.

---

## 8. Database

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

- `V1__init.sql` — all tables, FKs, indexes, sequences
- `V2__seed.sql` — the prototype's demo data: 8 categories, ~48 products,
  12 tables, 7 staff, 5 suppliers, 10 stock items

Set `spring.jpa.hibernate.ddl-auto=validate`.

Indexes to create explicitly: `orders(created_at)`, `orders(status)`,
`order_items(order_id)`, `products(category_id)`, `stock_movements(stock_item_id)`.

---

## 9. Local setup

```bash
docker compose up -d db
cd backend  && ./gradlew bootRun     # → http://localhost:8081
cd frontend && npm run dev           # → http://localhost:3000
```

Swagger: <http://localhost:8081/swagger-ui.html>

`frontend/.env.local`:

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081/api
```

Backend port stays **8081** to match the existing repo, leaving 3000 free for Next.

---

## 10. Build order

Each milestone should end in a runnable state.

| # | Milestone | Deliverable |
|---|---|---|
| 1 | **Scaffold** | Gradle + Next projects, docker-compose, health check green |
| 2 | **Schema** | Flyway V1 + V2, all entities, repositories |
| 3 | **Auth backend** | register/login/refresh/me + JWT filter + roles |
| 4 | **Auth frontend** | 5 auth screens, axios interceptor, protected routes |
| 5 | **App shell** | Sidebar/Topbar/StatusBar, both menus, role-based redirect |
| 6 | **UI kit** | the 11 components in §7.3, matching prototype styling |
| 7 | **Master data** | categories, products, tables, staff — CRUD both ends |
| 8 | **POS** | order screen, table picker, cart state, open order |
| 9 | **Payment** | payment screen, `/pay` transaction, receipt + print |
| 10 | **History** | order history with filters |
| 11 | **Supply chain** | suppliers, purchases, stock, adjustments |
| 12 | **Reports** | dashboard KPIs, charts, report tabs, CSV export |
| 13 | **Settings** | app settings, change password |
| 14 | **Hardening** | validation, error handling, tests, README |

Milestones 1–5 are sequential. 7–13 are independent once 6 lands.

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
- Bilingual labels `ខ្មែរ · English`, as in the prototype.
- Currency: USD primary, KHR secondary at a configurable rate (default 4100).

---

## 12. Open questions

1. **Khmer copy** — prototype text is placeholder. Needs the Figma section node
   IDs (`Ctrl+L` on the *Form Cashier* / *Form Admin* section) to extract real strings.
2. **Reuse or rewrite the NIEI-Y4 backend?** This spec assumes a fresh module
   with code carried over. Migrating the existing one in place is also viable —
   decide before milestone 1.
3. **i18n** — hardcoded bilingual strings (as prototyped), or `next-intl` with a
   language switcher?
4. **Printing** — browser print (as prototyped) or a thermal ESC/POS printer?
   The latter changes the receipt implementation substantially.
5. **Multi-tenancy** — `UserInfm.bizKey` suggests tenant scoping. Is that in scope?
