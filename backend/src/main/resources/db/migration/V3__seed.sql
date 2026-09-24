-- ---------------------------------------------------------------------------
-- V3 — reference and demo data, mirroring the HTML prototype.
--
-- No user accounts here: passwords must be BCrypt-hashed by the application, so
-- the initial admin is created idempotently by DataInitializer at startup.
--
-- Explicit IDs are used so foreign keys stay readable; each identity column is
-- restarted afterwards so future inserts do not collide.
-- ---------------------------------------------------------------------------

-- ===========================================================================
-- Categories
-- ===========================================================================

INSERT INTO category (id, name, name_en, icon, sort_order, status, reg_id, reg_dtm) VALUES
    (1, 'បាយ',      'Rice',    '🍚', 1, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (2, 'មី',        'Noodle',  '🍜', 2, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (3, 'សម្លរ',      'Soup',    '🍲', 3, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (4, 'ឆា',        'Fried',   '🍳', 4, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (5, 'សាច់អាំង',   'Grilled', '🍗', 5, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (6, 'សាល័ត',     'Salad',   '🥗', 6, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (7, 'ភេសជ្ជៈ',   'Drink',   '🥤', 7, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (8, 'បង្អែម',    'Dessert', '🍰', 8, 'INACTIVE', 'system', CURRENT_TIMESTAMP);

ALTER TABLE category ALTER COLUMN id RESTART WITH 9;

-- ===========================================================================
-- Products
-- ===========================================================================

INSERT INTO product (id, name, name_en, category_id, price, cost, stock_qty, image_url, status, reg_id, reg_dtm) VALUES
    (1,  'បាយឆា',            'Fried rice',           1, 4.50, 2.10, 42, '🍚', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (2,  'មីឆា',              'Fried noodle',         2, 3.50, 1.60, 38, '🍜', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (3,  'សម្លរប្រហើរ',        'Samlor prohor',        3, 5.00, 2.40, 25, '🍲', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (4,  'ស្ងោស្ពៃ',           'Boiled greens',        6, 3.00, 1.20, 19, '🥗', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (5,  'សាច់មាន់',           'Grilled chicken',      5, 7.50, 3.60, 14, '🍗', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (6,  'ត្រីចៀនជូរអែម',      'Sweet and sour fish',  4, 8.00, 3.90, 11, '🐟', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (7,  'បង្គាឆាម្ទេស',       'Chilli fried prawn',   4, 9.00, 4.50,  8, '🍤', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (8,  'សាច់គោលោកឡាក់',   'Lok lak beef',         4, 6.50, 3.10, 21, '🥩', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (9,  'ពងទាចៀន',          'Fried egg',            4, 2.00, 0.70, 60, '🍳', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (10, 'តែក្រូចឆ្មា',         'Lime tea',             7, 1.50, 0.58,  4, '🍵', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (11, 'កូកាកូឡា',          'Coca-Cola',            7, 1.00, 0.42, 56, '🥤', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (12, 'តែជ្រក់',            'Steeped tea',          7, 0.50, 0.18,  6, '🍵', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (13, 'ទឹកកក',             'Ice',                  7, 0.25, 0.05, 99, '🧊', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (14, 'ទឹកកកឯម',          'Ice cream',            8, 2.50, 1.00, 30, '🍧', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (15, 'នំខេក',             'Cake',                 8, 2.75, 1.10, 22, '🍰', 'ACTIVE', 'system', CURRENT_TIMESTAMP),
    (16, 'ផ្លែឈើចម្រុះ',       'Mixed fruit',          8, 3.25, 1.40, 18, '🍉', 'ACTIVE', 'system', CURRENT_TIMESTAMP);

ALTER TABLE product ALTER COLUMN id RESTART WITH 17;

-- ===========================================================================
-- Dining tables
-- ===========================================================================

INSERT INTO dining_table (id, name, seats, zone, status, reg_id, reg_dtm) VALUES
    (1,  'Table 01',  2, 'INDOOR',  'FREE',     'system', CURRENT_TIMESTAMP),
    (2,  'Table 02',  4, 'INDOOR',  'OCCUPIED', 'system', CURRENT_TIMESTAMP),
    (3,  'Table 03',  4, 'INDOOR',  'FREE',     'system', CURRENT_TIMESTAMP),
    (4,  'Table 04',  6, 'VIP',     'RESERVED', 'system', CURRENT_TIMESTAMP),
    (5,  'Table 05',  4, 'INDOOR',  'OCCUPIED', 'system', CURRENT_TIMESTAMP),
    (6,  'Table 06',  2, 'OUTDOOR', 'FREE',     'system', CURRENT_TIMESTAMP),
    (7,  'Table 07',  8, 'VIP',     'OCCUPIED', 'system', CURRENT_TIMESTAMP),
    (8,  'Table 08',  4, 'OUTDOOR', 'FREE',     'system', CURRENT_TIMESTAMP),
    (9,  'Table 09',  8, 'VIP',     'OCCUPIED', 'system', CURRENT_TIMESTAMP),
    (10, 'Table 10',  6, 'OUTDOOR', 'FREE',     'system', CURRENT_TIMESTAMP),
    (11, 'Table 11',  4, 'INDOOR',  'OCCUPIED', 'system', CURRENT_TIMESTAMP),
    (12, 'Table 12', 10, 'VIP',     'FREE',     'system', CURRENT_TIMESTAMP);

ALTER TABLE dining_table ALTER COLUMN id RESTART WITH 13;

-- ===========================================================================
-- Staff
-- ===========================================================================

INSERT INTO staff (id, staff_code, staff_name, gender, phone, role, shift, salary, hire_date, status, reg_id, reg_dtm) VALUES
    (1, 'EMP-001', 'Chan Sophea',   'FEMALE', '011 222 333', 'ADMIN',   'FULL_TIME', 600.00, DATE '2023-01-09', 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (2, 'EMP-014', 'Sok Dara',      'MALE',   '012 345 678', 'CASHIER', 'MORNING',   350.00, DATE '2024-03-11', 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (3, 'EMP-018', 'Chan Nita',     'FEMALE', '097 111 222', 'CASHIER', 'EVENING',   350.00, DATE '2024-06-03', 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (4, 'EMP-022', 'Ly Vuthy',      'MALE',   '092 888 444', 'CHEF',    'FULL_TIME', 480.00, DATE '2023-09-18', 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (5, 'EMP-027', 'Kim Srey Neat', 'FEMALE', '016 555 777', 'WAITER',  'MORNING',   280.00, DATE '2025-02-24', 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (6, 'EMP-031', 'Pich Rithy',    'MALE',   '078 909 121', 'WAITER',  'EVENING',   280.00, DATE '2025-05-12', 'ON_LEAVE', 'system', CURRENT_TIMESTAMP),
    (7, 'EMP-035', 'Meas Chanda',   'FEMALE', '089 343 565', 'CHEF',    'FULL_TIME', 450.00, DATE '2024-11-04', 'RESIGNED', 'system', CURRENT_TIMESTAMP);

ALTER TABLE staff ALTER COLUMN id RESTART WITH 8;

-- ===========================================================================
-- Suppliers
-- ===========================================================================

INSERT INTO supplier (id, supplier_code, company, contact_person, phone, supply_type, balance, status, reg_id, reg_dtm) VALUES
    (1, 'SUP-001', 'ក្រុមហ៊ុន មេគង្គ ហ្វូដ',    'Sok Vichea', '023 456 789', 'MEAT',      1240.00, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (2, 'SUP-002', 'ផ្សារទំនើប អាងតាមោក',     'Chea Sina',  '012 909 808', 'VEGETABLE',  460.00, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (3, 'SUP-003', 'Angkor Beverage Co., Ltd', 'Ny Rattana', '077 232 121', 'DRINK',     2050.00, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (4, 'SUP-004', 'ក្រុមហ៊ុន សមុទ្រ ស្រស់',    'Heng Pisey', '095 767 545', 'SEAFOOD',   3180.00, 'ACTIVE',   'system', CURRENT_TIMESTAMP),
    (5, 'SUP-005', 'Rice Land Cambodia',       'Tep Kosal',  '031 555 010', 'RICE',         0.00, 'INACTIVE', 'system', CURRENT_TIMESTAMP);

ALTER TABLE supplier ALTER COLUMN id RESTART WITH 6;

-- ===========================================================================
-- Stock items
-- ===========================================================================

INSERT INTO stock_item (id, name, unit, qty, min_qty, unit_cost, reg_id, reg_dtm) VALUES
    (1,  'អង្ករផ្កាម្លិះ', 'គីឡូក្រាម', 120.0, 30.0, 1.10, 'system', CURRENT_TIMESTAMP),
    (2,  'សាច់គោ',       'គីឡូក្រាម',   2.5, 10.0, 8.50, 'system', CURRENT_TIMESTAMP),
    (3,  'សាច់មាន់',      'គីឡូក្រាម',  28.0, 10.0, 5.00, 'system', CURRENT_TIMESTAMP),
    (4,  'បង្គា',         'គីឡូក្រាម',   3.0,  8.0, 12.00, 'system', CURRENT_TIMESTAMP),
    (5,  'ត្រីរស់',        'គីឡូក្រាម',  17.0,  6.0, 7.20, 'system', CURRENT_TIMESTAMP),
    (6,  'បន្លែចម្រុះ',    'គីឡូក្រាម',  45.0, 15.0, 1.80, 'system', CURRENT_TIMESTAMP),
    (7,  'បៀរអង្គរ',      'កំប៉ុង',       4.0, 48.0, 0.58, 'system', CURRENT_TIMESTAMP),
    (8,  'ទឹកសុទ្ធ',       'ដប',          6.0, 60.0, 0.18, 'system', CURRENT_TIMESTAMP),
    (9,  'កូកាកូឡា',      'កំប៉ុង',      96.0, 48.0, 0.42, 'system', CURRENT_TIMESTAMP),
    (10, 'ប្រេងឆា',       'លីត្រ',        0.0, 10.0, 2.40, 'system', CURRENT_TIMESTAMP);

ALTER TABLE stock_item ALTER COLUMN id RESTART WITH 11;

-- ===========================================================================
-- Application settings
-- ===========================================================================

INSERT INTO app_setting (setting_key, setting_value, description, reg_id, reg_dtm) VALUES
    ('restaurant.name',       'ភោជនីយដ្ឋាន អង្គរ',       'Restaurant name (Khmer)',        'system', CURRENT_TIMESTAMP),
    ('restaurant.nameEn',     'Angkor Restaurant',        'Restaurant name (English)',      'system', CURRENT_TIMESTAMP),
    ('restaurant.phone',      '012 345 678',              'Contact phone',                  'system', CURRENT_TIMESTAMP),
    ('restaurant.email',      'info@angkor-restaurant.com', 'Contact email',                'system', CURRENT_TIMESTAMP),
    ('restaurant.address',    'ផ្លូវ ២៧១ សង្កាត់ទួលទំពូង ខណ្ឌចំការមន រាជធានីភ្នំពេញ', 'Address', 'system', CURRENT_TIMESTAMP),
    ('currency.base',         'USD',                      'Base currency',                  'system', CURRENT_TIMESTAMP),
    ('currency.khrRate',      '4100',                     'USD to KHR exchange rate',       'system', CURRENT_TIMESTAMP),
    ('sales.vatRate',         '10',                       'VAT percentage',                 'system', CURRENT_TIMESTAMP),
    ('sales.invoicePrefix',   'INV-',                     'Invoice number prefix',          'system', CURRENT_TIMESTAMP),
    ('sales.purchasePrefix',  'PO-',                      'Purchase order number prefix',   'system', CURRENT_TIMESTAMP),
    ('option.autoPrint',      'true',                     'Auto print receipt after payment','system', CURRENT_TIMESTAMP),
    ('option.showKhr',        'true',                     'Show KHR alongside USD',         'system', CURRENT_TIMESTAMP),
    ('option.lowStockAlert',  'true',                     'Warn when stock drops below min','system', CURRENT_TIMESTAMP),
    ('option.requireTable',   'true',                     'Require table selection to order','system', CURRENT_TIMESTAMP),
    ('option.allowDiscount',  'false',                    'Allow manual discount at POS',   'system', CURRENT_TIMESTAMP);
