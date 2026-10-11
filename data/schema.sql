-- ========================================================
-- Apex Bank Management System - Complete Database Schema
-- Compatible with MySQL 8.0+ and MariaDB
-- ========================================================

CREATE DATABASE IF NOT EXISTS `bankdb` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `bankdb`;

-- 1. Drop existing tables in reverse dependency order
DROP TABLE IF EXISTS `transfers`;
DROP TABLE IF EXISTS `transactions`;
DROP TABLE IF EXISTS `accounts`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `customers`;

-- 2. Customers Table
CREATE TABLE `customers` (
    `customer_id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `address` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`customer_id`),
    UNIQUE KEY `uk_customer_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Users Table (Authentication & Role Based Access)
CREATE TABLE `users` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `customer_id` BIGINT DEFAULT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_customer` (`customer_id`),
    CONSTRAINT `fk_user_customer` FOREIGN KEY (`customer_id`) 
        REFERENCES `customers` (`customer_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Accounts Table
CREATE TABLE `accounts` (
    `account_id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_number` VARCHAR(12) NOT NULL,
    `customer_id` BIGINT NOT NULL,
    `account_type` VARCHAR(20) NOT NULL,
    `balance` DECIMAL(15,2) NOT NULL DEFAULT '0.00',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `version` BIGINT NOT NULL DEFAULT '0',
    PRIMARY KEY (`account_id`),
    UNIQUE KEY `uk_account_number` (`account_number`),
    KEY `idx_account_customer` (`customer_id`),
    CONSTRAINT `fk_account_customer` FOREIGN KEY (`customer_id`) 
        REFERENCES `customers` (`customer_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Transactions Table
CREATE TABLE `transactions` (
    `transaction_id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `balance_after` DECIMAL(15,2) NOT NULL,
    `date_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    `reference_no` VARCHAR(64) NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`transaction_id`),
    KEY `idx_transaction_account` (`account_id`),
    KEY `idx_transaction_ref` (`reference_no`),
    KEY `idx_transaction_date` (`date_time`),
    CONSTRAINT `fk_transaction_account` FOREIGN KEY (`account_id`) 
        REFERENCES `accounts` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Transfers Table (Inter-account transfer tracking)
CREATE TABLE `transfers` (
    `transfer_id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_account_id` BIGINT NOT NULL,
    `to_account_id` BIGINT NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `date_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    `reference_no` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`transfer_id`),
    UNIQUE KEY `uk_transfer_ref` (`reference_no`),
    KEY `idx_transfer_from` (`from_account_id`),
    KEY `idx_transfer_to` (`to_account_id`),
    CONSTRAINT `fk_transfer_from` FOREIGN KEY (`from_account_id`) 
        REFERENCES `accounts` (`account_id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_transfer_to` FOREIGN KEY (`to_account_id`) 
        REFERENCES `accounts` (`account_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
