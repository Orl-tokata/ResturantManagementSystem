-- Removes what scripts/generate-demo-sales.sql created.
--
--   psql -U rms -d rms -h localhost -f scripts/clean-demo-sales.sql
--
-- Safe to run when there is nothing to remove, and safe to run twice.
--
-- Matches on reg_id = 'demo-sales', which only the generator writes, so a real
-- sale cannot be caught by it however it was rung up. Nothing else needs
-- undoing: the generator does not touch stock, suppliers or anything outside
-- these two tables.
--
-- The invoice sequence is deliberately left where it is. Restarting it would
-- hand a future sale a number a deleted demo order already used, and a gap in
-- invoice numbering is ordinary where a duplicate is not.

\set ON_ERROR_STOP on

BEGIN;

\echo '=== about to remove ==='
SELECT count(*) AS orders, coalesce(sum(total), 0) AS revenue
FROM orders WHERE reg_id = 'demo-sales';

DELETE FROM order_item WHERE order_id IN (SELECT id FROM orders WHERE reg_id = 'demo-sales');
DELETE FROM orders WHERE reg_id = 'demo-sales';

\echo ''
\echo '=== orders left ==='
SELECT count(*) AS orders FROM orders;

COMMIT;
