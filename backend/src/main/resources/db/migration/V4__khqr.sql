-- KHQR payment.
--
-- A bill being shown a QR code is not the same as a bill being paid, so it
-- needs somewhere to sit in between. Without this an order would have to be
-- marked PAID the moment the code appeared, which is precisely the trust-based
-- behaviour this replaces.
--
-- One ALTER per column: PostgreSQL accepts a comma-separated list of ADD
-- COLUMN clauses and H2 does not, and the tests run on H2.

-- The MD5 of the QR payload. Bakong identifies a transaction by it, so it is
-- how this application asks "did that one settle?".
ALTER TABLE orders ADD COLUMN khqr_md5 VARCHAR(32);

-- The payload itself, kept so the same code can be redrawn if the till is
-- refreshed mid-payment. Regenerating would produce a different md5 and orphan
-- a customer who had already scanned.
ALTER TABLE orders ADD COLUMN khqr_payload VARCHAR(1024);

-- When the code stops being offered. A bill cannot be settled another way
-- while one is outstanding, so they are deliberately short-lived.
ALTER TABLE orders ADD COLUMN khqr_expires_at TIMESTAMP;

-- Who paid, as Bakong reports them, and the bank's own reference. Kept for
-- reconciliation: without these a KHQR line in the report cannot be matched
-- against a bank statement.
ALTER TABLE orders ADD COLUMN khqr_payer VARCHAR(120);
ALTER TABLE orders ADD COLUMN khqr_reference VARCHAR(120);

-- Checking a pending payment happens on a timer while a customer stands at the
-- till, so it must not be a table scan.
CREATE INDEX idx_orders_khqr_md5 ON orders (khqr_md5);
