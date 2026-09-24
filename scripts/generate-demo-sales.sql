-- Fills the reports with believable trading history.
--
--   psql -U rms -d rms -h localhost -f scripts/generate-demo-sales.sql
--
-- Removes with scripts/clean-demo-sales.sql. Re-running replaces what it made
-- last time rather than stacking a second fortnight on top.
--
-- Every row it writes carries reg_id = 'demo-sales'. That is the whole removal
-- story: no ids to remember, and a real sale can never be caught by it.
--
-- ---------------------------------------------------------------------------
-- Arithmetic matches OrderService.recalculate: line total is price x qty,
-- VAT applies to (subtotal - discount) at the rate in app_setting, and the
-- riel figure uses the configured exchange rate. If that calculation ever
-- changes, this drifts and the demo numbers stop matching what the app would
-- produce for the same basket.
--
-- Stock is deliberately NOT decremented. Two weeks of invented sales would
-- drive the seeded quantities negative and turn the low-stock panel into
-- nonsense; this exists to give the sales charts a shape, not to simulate
-- inventory.
-- ---------------------------------------------------------------------------

\set ON_ERROR_STOP on
\set days 14

BEGIN;

-- Same seed every run, so the charts look the same each time and a screenshot
-- keeps matching the data.
SELECT setseed(0.42);

-- Idempotent: clear the previous batch first.
DELETE FROM order_item WHERE order_id IN (SELECT id FROM orders WHERE reg_id = 'demo-sales');
DELETE FROM orders WHERE reg_id = 'demo-sales';

/* How busy each hour of the day is, relative to the others. Zero means closed.
   A lunch peak around noon and a heavier dinner peak in the evening is what
   makes the hourly chart worth looking at — a flat distribution would draw a
   rectangle and teach nobody anything. */
CREATE TEMP TABLE _hour_weight (hour int, weight int) ON COMMIT DROP;
INSERT INTO _hour_weight (hour, weight) VALUES
    (9, 1), (10, 2), (11, 6), (12, 10), (13, 8), (14, 3),
    (15, 2), (16, 2), (17, 4), (18, 8), (19, 10), (20, 7), (21, 3), (22, 1);

-- One row per order to create: a timestamp, a table and a payment method.
CREATE TEMP TABLE _plan (
    seq         bigserial,
    paid_at     timestamp,
    table_id    bigint,
    method      varchar(20)
) ON COMMIT DROP;

INSERT INTO _plan (paid_at, table_id, method)
SELECT
    (CURRENT_DATE - d * interval '1 day')
        + h.hour * interval '1 hour'
        + (floor(random() * 60))::int * interval '1 minute',
    1 + floor(random() * 12)::int,
    (ARRAY['CASH','CASH','CASH','CARD','KHQR','TRANSFER'])[1 + floor(random() * 6)::int]
FROM generate_series(0, :days - 1) AS d
CROSS JOIN _hour_weight h
-- The weight becomes a probability: a weight-10 hour nearly always produces an
-- order, a weight-1 hour rarely does. Weekends get more of everything.
CROSS JOIN generate_series(1, 2) AS rep
WHERE random() < (h.weight / 11.0)
    * CASE WHEN extract(dow FROM CURRENT_DATE - d * interval '1 day') IN (0, 6)
           THEN 1.35 ELSE 1.0 END;

-- The orders themselves. Totals are filled in once the lines exist.
INSERT INTO orders (invoice_no, table_id, cashier_id, guest_count,
                    subtotal, discount, vat_rate, vat_amount, total, total_khr,
                    payment_method, status, paid_at, act_yn, reg_id, reg_dtm)
SELECT
    'INV-' || lpad(nextval('seq_invoice_no')::text, 5, '0'),
    p.table_id,
    (SELECT id FROM users_infm WHERE user_id = 'cashier'),
    1 + floor(random() * 4)::int,
    0, 0,
    (SELECT setting_value::numeric FROM app_setting WHERE setting_key = 'sales.vatRate'),
    0, 0, 0,
    p.method,
    'PAID',
    p.paid_at,
    'Y',
    'demo-sales',
    p.paid_at
FROM _plan p;

/* Between one and four lines per order, drawn from the active menu. The
   distinct is what stops the same dish appearing twice on one bill, which the
   POS would not allow either — it merges a repeat tap into a quantity. */
INSERT INTO order_item (order_id, product_id, product_name, qty, unit_price, line_total)
SELECT DISTINCT ON (o.id, pr.id)
    o.id,
    pr.id,
    pr.name,
    q.qty,
    pr.price,
    round(pr.price * q.qty, 2)
FROM orders o
CROSS JOIN LATERAL (
    SELECT id, name, price
    FROM product
    WHERE status = 'ACTIVE'
    ORDER BY random()
    LIMIT 1 + floor(random() * 4)::int
) pr
CROSS JOIN LATERAL (SELECT 1 + floor(random() * 3)::int AS qty) q
WHERE o.reg_id = 'demo-sales';

-- Totals, exactly as the application computes them.
UPDATE orders o
SET subtotal   = s.subtotal,
    vat_amount = round(s.subtotal * o.vat_rate / 100, 2),
    total      = s.subtotal + round(s.subtotal * o.vat_rate / 100, 2),
    total_khr  = round((s.subtotal + round(s.subtotal * o.vat_rate / 100, 2))
                       * (SELECT setting_value::numeric FROM app_setting
                          WHERE setting_key = 'currency.khrRate'), 0)
FROM (SELECT order_id, sum(line_total) AS subtotal
      FROM order_item GROUP BY order_id) s
WHERE o.id = s.order_id AND o.reg_id = 'demo-sales';

-- Cash bills get a plausible note handed over and the change that follows.
UPDATE orders
SET amount_tendered = ceil(total / 5) * 5,
    change_amount   = ceil(total / 5) * 5 - total
WHERE reg_id = 'demo-sales' AND payment_method = 'CASH';

UPDATE orders
SET amount_tendered = total, change_amount = 0
WHERE reg_id = 'demo-sales' AND payment_method <> 'CASH';

\echo ''
\echo '=== generated ==='
SELECT count(*)                     AS orders,
       min(paid_at)::date            AS first_day,
       max(paid_at)::date            AS last_day,
       sum(total)                    AS revenue,
       round(avg(total), 2)          AS average_sale
FROM orders WHERE reg_id = 'demo-sales';

\echo ''
\echo '=== busiest hours ==='
SELECT extract(hour FROM paid_at)::int AS hour,
       count(*)                        AS orders,
       sum(total)                      AS revenue
FROM orders WHERE reg_id = 'demo-sales'
GROUP BY 1 ORDER BY revenue DESC LIMIT 6;

COMMIT;
