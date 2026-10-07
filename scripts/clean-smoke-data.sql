-- Removes what scripts/smoke-api.mjs leaves behind.
--
--   psql -U rms -d rms -h localhost -f scripts/clean-smoke-data.sql
--
-- Or paste it into any SQL client. It is safe to run when there is nothing to
-- clean, and safe to run twice.
--
-- Why this exists: against H2 the smoke test's records vanish on restart, but
-- against a real database they stay. Most of what it creates it also deletes;
-- what it cannot are the records the app deliberately protects — a stock item
-- with movement history, a supplier with an outstanding balance, a received
-- purchase order, a paid sale.
--
-- ---------------------------------------------------------------------------
-- It only ever deletes rows carrying the smoke test's own markers:
--
--     users_infm.user_id      LIKE 'smoke%'
--     supplier.supplier_code  LIKE 'SMOKE-%'
--     staff.staff_code        LIKE 'SMOKE-%'
--     stock_item.name         LIKE 'Smoke %'
--     dining_table.name       LIKE 'Smoke %'
--     orders.cashier_id       -> one of those users
--     idempotency_key.user_id LIKE 'smoke%'
--     customer.name           LIKE 'Smoke %'
--     promotion.name          LIKE 'Smoke %'
--     modifier_group.name     LIKE 'Smoke %'
--     product_variant.name    LIKE 'Smoke %'
--
-- Not "DELETE FROM orders". An earlier ad-hoc version of this did exactly that,
-- which was correct at the time because every order in the database happened to
-- be a smoke artefact — and would have destroyed a day of real sales the moment
-- anyone ran it on a database that had been used. A smoke order is only
-- distinguishable from a real one because the smoke test rings it up as the
-- account it registered, which is why it does that.
--
-- Change a marker in smoke-api.mjs and you must change it here too.
-- ---------------------------------------------------------------------------

\set ON_ERROR_STOP on

BEGIN;

-- The accounts the smoke test registered. Everything else keys off these.
CREATE TEMP TABLE _smoke_users ON COMMIT DROP AS
SELECT id FROM users_infm WHERE user_id LIKE 'smoke%';

CREATE TEMP TABLE _smoke_orders ON COMMIT DROP AS
SELECT id FROM orders WHERE cashier_id IN (SELECT id FROM _smoke_users);

CREATE TEMP TABLE _smoke_suppliers ON COMMIT DROP AS
SELECT id FROM supplier WHERE supplier_code LIKE 'SMOKE-%';

CREATE TEMP TABLE _smoke_stock ON COMMIT DROP AS
SELECT id FROM stock_item WHERE name LIKE 'Smoke %';

\echo '=== about to remove ==='
SELECT 'users'            AS what, count(*) FROM _smoke_users
UNION ALL SELECT 'orders',            count(*) FROM _smoke_orders
UNION ALL SELECT 'suppliers',         count(*) FROM _smoke_suppliers
UNION ALL SELECT 'stock items',       count(*) FROM _smoke_stock
UNION ALL SELECT 'purchases',         count(*) FROM purchase WHERE supplier_id IN (SELECT id FROM _smoke_suppliers)
UNION ALL SELECT 'stock movements',   count(*) FROM stock_movement WHERE stock_item_id IN (SELECT id FROM _smoke_stock)
UNION ALL SELECT 'tables',            count(*) FROM dining_table WHERE name LIKE 'Smoke %'
UNION ALL SELECT 'staff',             count(*) FROM staff WHERE staff_code LIKE 'SMOKE-%'
UNION ALL SELECT 'idempotency keys',  count(*) FROM idempotency_key WHERE user_id LIKE 'smoke%'
UNION ALL SELECT 'customers',         count(*) FROM customer WHERE name LIKE 'Smoke %'
UNION ALL SELECT 'promotions',        count(*) FROM promotion WHERE name LIKE 'Smoke %'
UNION ALL SELECT 'modifier groups',   count(*) FROM modifier_group WHERE name LIKE 'Smoke %'
UNION ALL SELECT 'variants',         count(*) FROM product_variant WHERE name LIKE 'Smoke %'
-- Rows that would stop the deletes rather than be removed by them. All of
-- these reference a smoke user or order with NO ACTION, so a count above zero
-- means this script will fail loudly and change nothing -- which is the right
-- outcome, and better found here than in the middle of it.
UNION ALL SELECT 'BLOCKER shifts',    count(*) FROM cash_shift WHERE user_ref IN (SELECT id FROM _smoke_users)
UNION ALL SELECT 'BLOCKER returns',   count(*) FROM sale_return WHERE order_id IN (SELECT id FROM _smoke_orders)
UNION ALL SELECT 'BLOCKER loyalty',   count(*) FROM loyalty_transaction WHERE order_id IN (SELECT id FROM _smoke_orders)
UNION ALL SELECT 'BLOCKER approvals', count(*) FROM sale_return WHERE approved_by IN (SELECT id FROM _smoke_users)
ORDER BY what;

-- Written in their own transaction by design, so they outlive the requests
-- that made them and are not removed by anything else. Harmless, but they
-- accumulate one row per smoke run per protected write.
DELETE FROM idempotency_key WHERE user_id LIKE 'smoke%';

-- ---------------------------------------------------------------------------
-- The stock is deliberately NOT given back
--
-- This used to add the smoke sales back to product.stock_qty, which was right
-- when that column was the only record of how much there was. V11 made it a
-- cached balance of the stock ledger, and the ledger is now the truth: every
-- product's stored figure equals the balance_after of its newest movement, and
-- StockLedgerTest asserts it.
--
-- Adding quantity here would write to the cache and not to the ledger, so the
-- two would disagree by exactly what the smoke test sold -- the drift P4 exists
-- to prevent, introduced by the script that is supposed to tidy up.
--
-- It is also not needed. V11's opening balances were taken from the quantities
-- as they stood, smoke sales already deducted, so the ledger and the shelf have
-- agreed since. A real correction belongs in the stock screen's Adjust modal,
-- where it leaves a movement with a reason on it.
-- ---------------------------------------------------------------------------

DELETE FROM order_item WHERE order_id IN (SELECT id FROM _smoke_orders);
DELETE FROM orders     WHERE id       IN (SELECT id FROM _smoke_orders);

DELETE FROM purchase_item WHERE purchase_id IN (SELECT id FROM purchase WHERE supplier_id IN (SELECT id FROM _smoke_suppliers));
DELETE FROM purchase      WHERE supplier_id IN (SELECT id FROM _smoke_suppliers);

DELETE FROM stock_movement WHERE stock_item_id IN (SELECT id FROM _smoke_stock);
DELETE FROM stock_item     WHERE id            IN (SELECT id FROM _smoke_stock);

DELETE FROM supplier     WHERE id IN (SELECT id FROM _smoke_suppliers);
DELETE FROM dining_table WHERE name LIKE 'Smoke %';
DELETE FROM staff        WHERE staff_code LIKE 'SMOKE-%';

DELETE FROM password_reset_token WHERE user_ref IN (SELECT id FROM _smoke_users);
DELETE FROM users_infm           WHERE id       IN (SELECT id FROM _smoke_users);

-- ---------------------------------------------------------------------------
-- What P6, P8 and P9 added
--
-- Each is deleted only when nothing surviving points at it. A real bill that
-- happened to use a smoke promotion, or a real customer registered with a
-- smoke-looking name, keeps its row: a cleanup script that can destroy real
-- history the moment a marker collides is worse than one that leaves a row
-- behind.
-- ---------------------------------------------------------------------------

DELETE FROM customer c
WHERE  c.name LIKE 'Smoke %'
  AND  NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.id)
  AND  NOT EXISTS (SELECT 1 FROM loyalty_transaction l WHERE l.customer_id = c.id);

DELETE FROM promotion p
WHERE  p.name LIKE 'Smoke %'
  AND  NOT EXISTS (SELECT 1 FROM orders o WHERE o.promotion_id = p.id)
  AND  NOT EXISTS (SELECT 1 FROM order_item i WHERE i.promotion_id = p.id);

DELETE FROM modifier_group g
WHERE  g.name LIKE 'Smoke %'
  AND  NOT EXISTS (SELECT 1 FROM order_item_modifier m
                   JOIN modifier x ON x.id = m.modifier_id
                   WHERE x.group_id = g.id);

-- A size the smoke test put on a seeded dish. Kept if a line was ever sold at
-- it, because order_item names the variant it was rung up as.
DELETE FROM product_variant v
WHERE  v.name LIKE 'Smoke %'
  AND  NOT EXISTS (SELECT 1 FROM order_item i WHERE i.variant_id = v.id);

\echo ''
-- A freshly seeded database reads: 9 categories, 18 products, 12 tables,
-- 7 staff, 5 suppliers, 10 stock items, 2 users, and no orders or
-- purchases. Anything above those is real work somebody did, not residue.
\echo '=== what is left ==='
SELECT 'categories'  AS what, count(*) FROM category
UNION ALL SELECT 'products',    count(*) FROM product
UNION ALL SELECT 'tables',      count(*) FROM dining_table
UNION ALL SELECT 'staff',       count(*) FROM staff
UNION ALL SELECT 'suppliers',   count(*) FROM supplier
UNION ALL SELECT 'stock items', count(*) FROM stock_item
UNION ALL SELECT 'users',       count(*) FROM users_infm
UNION ALL SELECT 'orders',      count(*) FROM orders
UNION ALL SELECT 'purchases',   count(*) FROM purchase
ORDER BY what;

COMMIT;

-- Deliberately not reset: the identity counters and the invoice/PO sequences.
-- Restarting a sequence that a surviving real order already used would hand out
-- a duplicate invoice number, which is worse than a gap. Gaps in invoice
-- numbering are normal; collisions are not. On a database used only for smoke
-- runs you can reset them by hand:
--
--   ALTER SEQUENCE seq_invoice_no  RESTART WITH 1;
--   ALTER SEQUENCE seq_purchase_no RESTART WITH 1;
--   ALTER TABLE orders ALTER COLUMN id RESTART WITH 1;
