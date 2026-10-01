-- ---------------------------------------------------------------------------
-- V9 — product.image_url becomes product.icon
--
-- The column was named for a URL and has never held one. Every row in it is a
-- single emoji:
--
--     Hot coffee   image_url = '☕'
--     Milk coffee  image_url = '☕'
--
-- and both the POS grid and the product table render it as text, not as an
-- image. A name that describes something the column has never contained is a
-- trap for whoever reads this schema next and writes code expecting a src.
--
-- Renaming rather than adding: there is no data to migrate and nothing to keep
-- compatible, since the only client ships with the server.
--
-- 16 characters rather than 255. An emoji is one codepoint, or several when it
-- carries a skin tone or a zero-width joiner — 👨‍🍳 is five. Sixteen leaves room
-- for any of those and stops the column from quietly becoming a text field
-- again.
--
-- If photographs are wanted later, they want their own column: a URL and an
-- emoji are different things, and a product can sensibly have both — the emoji
-- showing instantly while the picture loads, and standing in when there is no
-- picture at all.
-- ---------------------------------------------------------------------------

ALTER TABLE product RENAME COLUMN image_url TO icon;

ALTER TABLE product ALTER COLUMN icon TYPE VARCHAR(16);
