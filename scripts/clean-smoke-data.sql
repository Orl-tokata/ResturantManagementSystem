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
ORDER BY what;

-- Give back the stock those smoke sales consumed. Has to run before the order
-- lines go, because it reads them; without it the inventory stays permanently
-- short by whatever the smoke test sold.
UPDATE product p
SET    stock_qty = p.stock_qty + s.sold
FROM  (SELECT i.product_id, sum(i.qty) AS sold
       FROM   order_item i
       WHERE  i.order_id IN (SELECT id FROM _smoke_orders)
       GROUP  BY i.product_id) s
WHERE p.id = s.product_id;

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

\echo ''
\echo '=== what is left (a freshly seeded database reads 9 / 18 / 12 / 7 / 5 / 10 / 2) ==='
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
