-- ---------------------------------------------------------------------------
-- V11 — a stock ledger that can explain itself
--
-- Selling a dish decremented product.stock_qty and wrote nothing. There was no
-- way to answer "why is this count wrong", because nothing recorded the
-- decrements — the single most useful question a stock screen can answer, and
-- the one this system could not.
--
-- stock_movement already existed but only for stock_item, the ingredients
-- table: three types (IN, OUT, DAMAGED), no reference to the document that
-- caused the movement, and no way to name a product at all.
--
-- ---------------------------------------------------------------------------
-- One ledger, not two
--
-- product_id joins stock_item_id rather than getting a table of its own.
-- "Everything that moved, and why" is one question, and answering it across two
-- tables means every report learning about both. A row names exactly one of
-- them, which the CHECK enforces rather than trusting.
--
-- ---------------------------------------------------------------------------
-- Direction stays in the type
--
-- qty remains positive and IN/OUT/DAMAGED keep their meanings; SALE and RETURN
-- join them. Re-signing the quantity would have been tidier on a blank sheet
-- and would have meant rewriting every existing row to mean what it already
-- means. docs/ERD.md §1.4 made the same call.
--
-- ---------------------------------------------------------------------------
-- Opening balances, not invented history
--
-- balance_after is what makes the ledger worth reading: it is the number the
-- screen showed after that movement, so a wrong count can be traced to the row
-- that made it wrong.
--
-- It cannot be filled in for movements already recorded. The balance before the
-- first of them was never written down, so any figure here would be a guess
-- presented as a record. Those rows keep NULL and the screen shows a dash.
--
-- Instead every product and stock item gets one opening-balance movement at
-- today's quantity. From that row onward the invariant holds and is testable:
-- the newest movement's balance_after equals the stored quantity.
-- ---------------------------------------------------------------------------

ALTER TABLE stock_movement ADD COLUMN product_id BIGINT;
ALTER TABLE stock_movement ADD COLUMN ref_type VARCHAR(30);
ALTER TABLE stock_movement ADD COLUMN ref_id BIGINT;
ALTER TABLE stock_movement ADD COLUMN balance_after NUMERIC(12, 2);

ALTER TABLE stock_movement ALTER COLUMN stock_item_id DROP NOT NULL;

ALTER TABLE stock_movement
    ADD CONSTRAINT fk_movement_product FOREIGN KEY (product_id) REFERENCES product (id);

-- A movement is about one thing. Without this a row could name both and the
-- two balances would disagree, or name neither and belong to nothing.
ALTER TABLE stock_movement
    ADD CONSTRAINT ck_movement_subject CHECK (
        (product_id IS NOT NULL AND stock_item_id IS NULL)
     OR (product_id IS NULL AND stock_item_id IS NOT NULL));

-- ---------------------------------------------------------------------------
-- A quantity of zero
--
-- ck_movement_qty required qty > 0, which is right for every movement that
-- describes something happening: a sale of nothing, a correction of nothing.
--
-- An opening balance is not that. "As of now this product holds zero" is a
-- real statement, and several products do hold zero — without the row there is
-- no way to tell a product whose ledger starts empty from one whose ledger
-- never started, and the balance invariant has a hole in it.
--
-- So the column permits zero and StockLedger.require still refuses it, which
-- means zero can only ever arrive from an opening balance written here.
-- ---------------------------------------------------------------------------

ALTER TABLE stock_movement DROP CONSTRAINT ck_movement_qty;
ALTER TABLE stock_movement ADD CONSTRAINT ck_movement_qty CHECK (qty >= 0);

ALTER TABLE stock_movement DROP CONSTRAINT ck_movement_type;
ALTER TABLE stock_movement
    ADD CONSTRAINT ck_movement_type CHECK (movement_type IN (
        'IN',        -- received, or a manual increase
        'OUT',       -- a manual decrease
        'DAMAGED',   -- spoiled, broken or lost
        'SALE',      -- left as part of a settled bill
        'RETURN'));  -- came back from one

CREATE INDEX ix_movement_product ON stock_movement (product_id, created_at);
CREATE INDEX ix_movement_ref ON stock_movement (ref_type, ref_id);

-- ---------------------------------------------------------------------------
-- Opening balances
--
-- Dated now, not backdated: this is the moment the ledger starts, and pretending
-- otherwise would put a number in the past that nobody recorded.
-- ---------------------------------------------------------------------------

INSERT INTO stock_movement
    (product_id, stock_item_id, movement_type, qty, reason, balance_after, created_by, created_at)
SELECT p.id, NULL, 'IN', p.stock_qty, 'Opening balance (V11)', p.stock_qty,
       'system', CURRENT_TIMESTAMP
FROM   product p
WHERE  p.stock_qty IS NOT NULL;

INSERT INTO stock_movement
    (product_id, stock_item_id, movement_type, qty, reason, balance_after, created_by, created_at)
SELECT NULL, s.id, 'IN', s.qty, 'Opening balance (V11)', s.qty,
       'system', CURRENT_TIMESTAMP
FROM   stock_item s
WHERE  s.qty IS NOT NULL;
