-- ---------------------------------------------------------------------------
-- V1 — baseline
--
-- Tables land in V2 (milestone 2, "Schema"). This migration only creates the
-- document-number sequences, so invoice and purchase-order numbers are handed
-- out by the database rather than derived from count()+1, which races under
-- concurrent cashiers.
-- ---------------------------------------------------------------------------

CREATE SEQUENCE IF NOT EXISTS seq_invoice_no
    START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS seq_purchase_no
    START WITH 1 INCREMENT BY 1 NO CYCLE;

COMMENT ON SEQUENCE seq_invoice_no  IS 'Backs Order.invoiceNo, formatted INV-%05d';
COMMENT ON SEQUENCE seq_purchase_no IS 'Backs Purchase.poNo, formatted PO-%05d';
