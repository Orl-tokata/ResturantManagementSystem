# Step 2 — Screen List

Follows [ARCHITECTURE.md](ARCHITECTURE.md). Same assumption: evolve the 22
screens that exist, do not redraw them.

Roles today are exactly four: `ADMIN`, `CASHIER`, `WAITER`, `CHEF` — enforced
by a CHECK constraint, not just the Java enum. **`MANAGER` does not exist.**
Where it appears below it is a proposal, and it needs a migration to the CHECK
on both `users_infm` and `staff`. §7 covers permissions becoming data.

---

## 1. What exists

22 user-facing pages. Auth is complete; the rest splits admin / cashier.

### Auth — 4 screens, done

`/login` · `/forgot-password` · `/verify-otp` · `/reset-password`

OTP by email works. Nothing here changes.

### Admin — 11 screens

| Route | Covers | Brief says |
|---|---|---|
| `/admin` | KPI tiles, 30-day sales, best sellers, low stock | keep |
| `/admin/products` | menu items, price, stock, barcode | **extend** — variants, modifiers, cost |
| `/admin/categories` | category CRUD | keep |
| `/admin/tables` | dining tables | restaurant-only; brief omits it |
| `/admin/staff` | HR records | **extend** — link to a login account |
| `/admin/suppliers` | supplier CRUD | keep |
| `/admin/purchase` | goods received | **extend** — movements, warehouse |
| `/admin/stock` | ingredient levels + adjustments | **rework** — see §3.2 |
| `/admin/reports` | sales reports | **extend** — shift, profit, tax |
| `/admin/settings` | shop name, currency, tax, KHQR | **extend** — branch, printer |
| `/admin/change-password` | self-service | keep |

`/admin/ui-kit` also exists — a component gallery, developer-facing. Keep it
out of the nav.

### Cashier — 7 screens

`/cashier` home · `/cashier/order` POS · `/cashier/tables` ·
`/cashier/payment` · `/cashier/receipt` + `/receipt/[id]` ·
`/cashier/history` · `/cashier/profile`

---

## 2. Design issues before the screen list

Three things that adding the brief's screens would make worse, per its rule 15.

### 2.1 One sale currently spans four screens

To take a payment a cashier walks `/tables → /order → /payment → /receipt`.
That is four page loads and three navigations for one transaction, and it is
the single most repeated action in the building.

> **Recommend collapsing payment into the order screen** as a panel, not a
> route. Table pick stays separate — it is genuinely a different decision —
> and the receipt stays a route because it must be linkable and reprintable.
>
> Two screens, not four. The brief's retail flow is one screen for exactly this
> reason; it just drew the wrong conclusion about tables.

This is the highest-value UI change in the plan and it removes code rather than
adding it.

### 2.2 The admin sidebar is a flat list of 11, and the brief adds 10

Customers, promotions, loyalty, returns, shifts, branches, users, roles, audit,
tax. A flat 21-item sidebar is a scroll, and a scroll is a menu nobody reads.

> **Group it.** Five headings, matching how an owner actually thinks:
>
> - **Sell** — dashboard, sales history, returns, shifts
> - **Catalog** — products, categories, modifiers, promotions
> - **Stock** — levels, movements, purchases, suppliers
> - **People** — customers, loyalty, staff, users, roles
> - **Setup** — branches, tax, printers, settings, audit log
>
> Collapse all but the open group. Cashier nav stays flat — it is 7 items and
> a cashier learns them in a day.

### 2.3 Kitchen display is not a page in the cashier shell

The brief lists it beside the others. It is a different device: a wall screen,
three metres away, no keyboard, nobody touching it, running unattended for
twelve hours.

> Give it its own route group and layout — no sidebar, no clock, no language
> switcher, huge type, dark background, auto-refresh. `/kitchen`, full-bleed.
> Putting it in the cashier shell inherits chrome that is wrong in every
> particular.

---

## 3. New screens

### 3.1 Shift — blocks everything else · **built (P3)**

| | |
|---|---|
| Route | `/cashier/shift` |
| Roles | CASHIER, MANAGER, ADMIN |
| Purpose | Open with a counted float; close with a counted drawer |

**Open** — one number, the starting cash, and a Start button. That is all.

**Close** — declared cash against expected, the variance shown plainly with no
euphemism, a note field when it is non-zero, then a Z-report to print.

**This screen is a gate.** A cashier with no open shift lands here after login
and cannot reach the POS. Without that rule every cash report is fiction, so it
earns the friction — but it must be two taps, or people will work around it.

States: no shift · open (elapsed, sales so far) · closing (counting) ·
closed (read-only Z-report).

### 3.2 Stock movements — replaces the current stock screen

| | |
|---|---|
| Route | `/admin/stock` (rework) + `/admin/stock/movements` |
| Roles | ADMIN, MANAGER |

Today this screen shows a mutable level and lets someone type a new one.
ARCHITECTURE §6 explains why that cannot stay.

- **Levels** — current quantity per item, low-stock first. Read-only. The
  number here is now a *derived* figure, and showing it in an input box lies
  about what it is.
- **Movements** — the ledger. Date, item, type (`PURCHASE` `SALE` `ADJUST`
  `TRANSFER` `WASTE` `RETURN`), signed quantity, reference document, who,
  balance after. Filterable by item and date. Never editable.
- **Adjust** — a modal writing a movement with a mandatory reason. Waste,
  breakage and recount are the three real cases; make them preset buttons.

The "balance after" column is what makes this screen worth building. It answers
"why is this count wrong", and today there is no answer at all.

### 3.3 Customers · **built (P6)**

| | |
|---|---|
| Route | `/admin/customers`, `/admin/customers/[id]` |
| Roles | ADMIN, MANAGER |

List: name, phone, points, last visit, total spent. Phone is the natural key in
Cambodia — more reliable than email, and it is what a cashier can ask for at
the till. Detail: purchase history, points ledger, notes.

At the POS this is a search-by-phone in the order panel, not a page.

### 3.4 Returns · **built (P7)**

| | |
|---|---|
| Route | `/cashier/returns/[saleId]` |
| Roles | CASHIER (limited), MANAGER |

Start from the original sale — never a blank form, or the screen becomes a way
to take money out of the drawer. Pick lines and quantities, reason mandatory,
refund method defaults to the original tender.

A return must write: negative sale lines, a reversing stock movement per line,
a cash movement against the open shift, and a loyalty reversal — all in the one
transaction described in ARCHITECTURE §4.2.

> **Manager approval above a threshold.** Not because cashiers are dishonest,
> but because the audit log needs a name against a refund, and this is the
> cheapest place to capture it.

### 3.5 Promotions

| | |
|---|---|
| Route | `/admin/promotions` |
| Roles | ADMIN, MANAGER |

Percent off · fixed amount off · buy-X-get-Y · happy hour by time window.
Scope: item, category, or whole bill. Date range, active toggle.

> **Ship percent-and-fixed first.** Buy-X-get-Y interacts with returns, loyalty
> and tax in ways that are not obvious, and a wrong discount is a wrong price
> on a printed receipt. The two simple types cover most of what a shop runs.

### 3.6 Branches

| | |
|---|---|
| Route | `/admin/branches` |
| Roles | ADMIN |

Name, address, phone, active. Plus a **branch switcher in the header** for
users with more than one — and a badge showing which branch you are looking at,
on every screen, permanently.

The failure mode is someone adjusting stock in the wrong branch and not
noticing for a week. A persistent badge is cheap insurance.

### 3.7 Users and roles

| | |
|---|---|
| Route | `/admin/users`, `/admin/roles` |
| Roles | ADMIN |

Users: account, role, branch, active, last login. Separate from `/admin/staff`,
which is an HR record and does not imply a login — that distinction already
exists in the code and is worth keeping visible.

Roles: permission matrix, checkbox grid. Only once permissions are data
(ARCHITECTURE §5); built before that, this screen would be a lie.

### 3.8 Audit log

| | |
|---|---|
| Route | `/admin/audit` |
| Roles | ADMIN |

Who, what, when, before → after. Filter by user, entity, date. Read-only, no
delete, ever. Mostly unvisited — and exactly the thing you want on the day
something is wrong.

### 3.9 Kitchen display

| | |
|---|---|
| Route | `/kitchen` — own layout, no shell |
| Roles | CHEF |

Columns: New · Cooking · Ready. Cards carry table, items, modifiers, elapsed
time. A card turns amber past 10 minutes and red past 20 — that is the whole
reason the screen exists.

Polls until WebSocket lands (ARCHITECTURE §7). One tap advances a card. Tap
targets no smaller than 80px: the people using this have wet hands.

### 3.10 Staff attendance — Phase 2

| | |
|---|---|
| Route | `/admin/attendance`, `/admin/attendance/[staffId]` |
| Roles | ADMIN, MANAGER |

A board of who is in now, and a month view per person. Clock in and out record
raw times and nothing else; the arithmetic that turns hours into pay stays with
a person, because Cambodia's overtime and holiday rules are specific enough
that computing it wrongly is a liability rather than a feature.

**Cashiers do not clock in.** §3.1 already makes opening a shift a gate — no
open shift, no POS — so `cash_shift.opened_at` is their arrival, recorded
because they cannot sell without it. A second ritual for the same arrival is
the one people skip, and attendance nobody maintains is worse than none: it
looks authoritative and is not. Manual clock-in is for staff with no till.

Corrections go through the audit log. Attendance a manager can edit silently is
worthless the moment there is a dispute, and that is the only moment it matters.

Scoped and sequenced in PLAN.md §4a Q2. Not in the pasted brief — an addition,
and worth naming as a choice.

---

## 4. Screens the brief asks for that I would not build

| Brief screen | Why not |
|---|---|
| Barcode-scan-first POS | ARCHITECTURE §1.3 — dishes have no barcode. Keep name-and-category search; a scanner still works, as keyboard input into that same box |
| Separate variant manager | Variants belong inside the product form. A second screen means two places to look for one product's price |
| Warehouse transfer | Not until there are two warehouses. One shop, one store room |
| Customer-facing display | Real, but it is a second monitor and a second deployment. Phase 3 at the earliest |

---

## 5. Cambodia-specific rules, applied to every screen

Not features — constraints that change how each screen is drawn. Getting these
wrong is more visible than a missing report.

1. **Dual currency everywhere.** USD is the unit of account; KHR is what change
   is given in. Every total shows both. Riel rounds to the nearest 100 — a
   total of ៛4,237 is not a price anyone has ever paid.
2. **The exchange rate is dated.** ARCHITECTURE §10. A receipt reprinted next
   month must show the rate that applied on the day, so the screen reads it
   from the sale, not from settings.
3. **Khmer and English side by side** where space allows, Khmer first when the
   locale is Khmer. Already built — `useBilingual()`.
4. **Khmer needs vertical room.** The script stacks; a line box sized for Latin
   clips subscripts. This has already bitten this project once, when the
   receipt font fell back silently and dropped glyphs.
5. **Touch first on the POS.** Assume a 10-inch tablet and a thumb. 44px
   minimum on anything tapped during service.
6. **Printing is 80mm thermal.** Solved for receipts. Reports meant for paper
   need the same treatment, or they come out stretched across A4 — a bug this
   project has already shipped and fixed once.
7. **The network will drop.** Not a maybe. See §6.

---

## 6. Offline — scope it honestly

Full offline POS means conflict resolution, and conflict resolution on money is
a genuinely hard problem that has sunk larger projects than this one.

> **Recommend a middle position for Phase 1:**
>
> - Cache the menu so the POS *renders* without the network.
> - Queue nothing. If the connection is down, say so in a banner — loudly,
>   unmissably — and let the cashier write the order on paper as they already
>   would.
> - Never let a sale appear to succeed when it did not reach the server. That
>   is worse than any outage.
>
> Full offline-first is Phase 4, if ever, and needs its own design.

KHQR deserves a note: the *customer's* phone talks to the bank directly, so a
QR on screen can be paid even while the till cannot verify it. That is exactly
what `UNVERIFIABLE` is for — the till says "cannot confirm" instead of
guessing, and the cashier decides.

---

## 7. Screen count

| | Now | Phase 1 | Later |
|---|---|---|---|
| Auth | 4 | 4 | 4 |
| Admin | 11 | 18 | 23 |
| Cashier | 7 | 6 | 6 |
| Kitchen | 0 | 0 | 1 |
| **Total** | **22** | **28** | **34** |

Later gained two: the attendance board and its per-person month view (§3.10).

Cashier goes *down* by one — that is §2.1, payment folding into the order
screen. It is the only count in this document I am pleased about.

---

## Next

Step 3 is the ERD, and it has to agree with this list: every screen above needs
tables behind it, and shift, movement and audit are the three that decide
whether the rest is buildable.

Worth settling first: §2.1 (collapsing payment) and §6 (offline scope). Both
change what the data model has to carry.
