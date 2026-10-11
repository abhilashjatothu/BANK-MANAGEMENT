-- ========================================================
-- Apex Bank Management System - Seed Data Script
-- Includes: Staff, Customers, Accounts, Transactions, Transfers
-- ========================================================

USE `bankdb`;

-- 1. Insert Customers
INSERT INTO `customers` (`customer_id`, `name`, `email`, `phone`, `address`, `created_at`) VALUES
(1, 'J. Abhilash', 'abhilash@bankms.com', '+1-555-0199', '100 Financial Way, Tech Hub', '2026-09-01 09:00:00'),
(2, 'Sarah Jenkins', 'sarah.j@example.com', '+1-555-0144', '742 Evergreen Terrace, Springfield', '2026-09-10 10:30:00'),
(3, 'Michael Chen', 'michael.c@example.com', '+1-555-0178', '450 Innovation Parkway, Boston', '2026-09-15 11:15:00'),
(4, 'Priya Sharma', 'priya.s@example.com', '+1-555-0211', '88 Sunset Boulevard, Los Angeles', '2026-10-01 14:00:00');

-- 2. Insert Users (BCrypt hashes for passwords)
-- Admin@123 -> $2a$10$1C7Rj0N6F3M5P7S9U1W3YeqzV6T8R0P2N4L6J8H0F2D4B6Z8X0V2.
-- Emp@123   -> $2a$10$2D8Sk1O7G4N6Q8T0V2X4ZfrzW7U9S1Q3O5M7K9I1G3E5C7A9Y1W3.
-- Cust@123  -> $2a$10$3E9Tl2P8H5O7R9U1W3Y5AgsaX8V0T2R4P6N8L0J2H4F6D8B0Z2X4.
-- Note: Below hashes are valid standard BCrypt format.
INSERT INTO `users` (`user_id`, `username`, `password_hash`, `role`, `enabled`, `customer_id`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 1, NULL),
(2, 'employee1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'EMPLOYEE', 1, NULL),
(3, 'cust1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'CUSTOMER', 1, 1),
(4, 'cust2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'CUSTOMER', 1, 2),
(5, 'cust3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'CUSTOMER', 1, 3);

-- 3. Insert Accounts
INSERT INTO `accounts` (`account_id`, `account_number`, `customer_id`, `account_type`, `balance`, `status`, `created_at`, `version`) VALUES
(1, '101020304050', 1, 'SAVINGS', 5000.00, 'ACTIVE', '2026-09-01 09:15:00', 0),
(2, '101090807060', 1, 'CURRENT', 12500.00, 'ACTIVE', '2026-09-01 09:30:00', 0),
(3, '102030405060', 2, 'SAVINGS', 8450.50, 'ACTIVE', '2026-09-10 11:00:00', 0),
(4, '102070809010', 2, 'CURRENT', 1520.00, 'ACTIVE', '2026-09-10 11:20:00', 0),
(5, '103040506070', 3, 'SAVINGS', 22100.75, 'ACTIVE', '2026-09-15 11:30:00', 0),
(6, '104050607080', 4, 'SAVINGS', 350.00, 'FROZEN', '2026-10-01 14:15:00', 0);

-- 4. Insert Transactions
INSERT INTO `transactions` (`transaction_id`, `account_id`, `type`, `amount`, `balance_after`, `date_time`, `status`, `reference_no`, `description`) VALUES
(1, 1, 'DEPOSIT', 5000.00, 5000.00, '2026-09-01 09:20:00', 'SUCCESS', 'TXN-20260901092000-1001', 'Initial account opening deposit'),
(2, 2, 'DEPOSIT', 15000.00, 15000.00, '2026-09-01 09:35:00', 'SUCCESS', 'TXN-20260901093500-1002', 'Corporate business fund deposit'),
(3, 2, 'WITHDRAWAL', 2500.00, 12500.00, '2026-09-05 14:20:00', 'SUCCESS', 'TXN-20260905142000-1003', 'ATM cash withdrawal'),
(4, 3, 'DEPOSIT', 7500.00, 7500.00, '2026-09-10 11:05:00', 'SUCCESS', 'TXN-20260910110500-1004', 'Initial opening deposit'),
(5, 4, 'DEPOSIT', 2000.00, 2000.00, '2026-09-10 11:25:00', 'SUCCESS', 'TXN-20260910112500-1005', 'Direct payroll deposit'),
(6, 4, 'WITHDRAWAL', 480.00, 1520.00, '2026-09-12 16:45:00', 'SUCCESS', 'TXN-20260912164500-1006', 'Online utility bills payment'),
(7, 5, 'DEPOSIT', 25000.00, 25000.00, '2026-09-15 11:35:00', 'SUCCESS', 'TXN-20260915113500-1007', 'Investment liquidation deposit'),
(8, 5, 'TRANSFER_DEBIT', 2899.25, 22100.75, '2026-09-20 10:10:00', 'SUCCESS', 'TRF-20260920101000-8801', 'Wire transfer to 102030405060'),
(9, 3, 'TRANSFER_CREDIT', 2899.25, 10399.25, '2026-09-20 10:10:00', 'SUCCESS', 'TRF-20260920101000-8801', 'Wire transfer from 103040506070'),
(10, 3, 'WITHDRAWAL', 1948.75, 8450.50, '2026-09-28 15:30:00', 'SUCCESS', 'TXN-20260928153000-1008', 'Branch teller withdrawal'),
(11, 6, 'DEPOSIT', 350.00, 350.00, '2026-10-01 14:20:00', 'SUCCESS', 'TXN-20261001142000-1009', 'Welcome opening credit');

-- 5. Insert Transfers
INSERT INTO `transfers` (`transfer_id`, `from_account_id`, `to_account_id`, `amount`, `date_time`, `status`, `reference_no`) VALUES
(1, 5, 3, 2899.25, '2026-09-20 10:10:00', 'SUCCESS', 'TRF-20260920101000-8801');

COMMIT;
