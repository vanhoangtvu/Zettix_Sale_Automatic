-- Zettix Database Initialization Script

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS zettix_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE zettix_db;

-- Create admin user
INSERT IGNORE INTO users (username, email, password, full_name, role, is_active, is_locked, created_at, updated_at) 
VALUES ('admin', 'admin@zettix.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Zettix Admin', 'ADMIN', true, false, NOW(), NOW());

-- Create sample products
INSERT IGNORE INTO products (name, description, product_type, price, is_active, total_quantity, available_quantity, sold_quantity, category, created_at, updated_at) 
VALUES 
('Netflix Premium 1 Month', 'Netflix Premium account for 1 month', 'ACCOUNT', 50000.00, true, 100, 100, 0, 'Streaming', NOW(), NOW()),
('Spotify Premium 1 Month', 'Spotify Premium account for 1 month', 'ACCOUNT', 30000.00, true, 100, 100, 0, 'Music', NOW(), NOW()),
('Adobe Creative Cloud 1 Month', 'Adobe Creative Cloud license for 1 month', 'LICENSE_KEY', 200000.00, true, 50, 50, 0, 'Software', NOW(), NOW()),
('Steam Game Key - Cyberpunk 2077', 'Steam game key for Cyberpunk 2077', 'LICENSE_KEY', 500000.00, true, 20, 20, 0, 'Gaming', NOW(), NOW());

-- Create sample accounts for testing
INSERT IGNORE INTO accounts (product_id, username, password, email, additional_info, status, created_at, updated_at) 
SELECT 
    p.id,
    CONCAT('netflix_', FLOOR(RAND() * 1000)),
    CONCAT('pass_', FLOOR(RAND() * 10000)),
    CONCAT('netflix', FLOOR(RAND() * 1000), '@example.com'),
    'Premium account - 1 month remaining',
    'AVAILABLE',
    NOW(),
    NOW()
FROM products p WHERE p.name = 'Netflix Premium 1 Month' LIMIT 10;

INSERT IGNORE INTO accounts (product_id, username, password, email, additional_info, status, created_at, updated_at) 
SELECT 
    p.id,
    CONCAT('spotify_', FLOOR(RAND() * 1000)),
    CONCAT('pass_', FLOOR(RAND() * 10000)),
    CONCAT('spotify', FLOOR(RAND() * 1000), '@example.com'),
    'Premium account - 1 month remaining',
    'AVAILABLE',
    NOW(),
    NOW()
FROM products p WHERE p.name = 'Spotify Premium 1 Month' LIMIT 10;

-- Update product quantities
UPDATE products SET 
    total_quantity = (SELECT COUNT(*) FROM accounts WHERE product_id = products.id),
    available_quantity = (SELECT COUNT(*) FROM accounts WHERE product_id = products.id AND status = 'AVAILABLE')
WHERE id IN (SELECT DISTINCT product_id FROM accounts);
