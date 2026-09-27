-- Let an order sit in AWAITING_PAYMENT.
--
-- V4 added the KHQR columns but left the status check constraint from V2
-- alone, which still only permitted OPEN, PAID and CANCELLED. Showing a code
-- therefore failed at the database with a constraint violation — the check
-- doing exactly its job, and catching a value the application had started
-- using without telling the schema.
--
-- A separate migration rather than an edit to V4: V4 has already run, so
-- changing it now is a repair on every database that has it, not an edit.

ALTER TABLE orders DROP CONSTRAINT ck_orders_status;

ALTER TABLE orders ADD CONSTRAINT ck_orders_status
    CHECK (status IN ('OPEN', 'AWAITING_PAYMENT', 'PAID', 'CANCELLED'));
