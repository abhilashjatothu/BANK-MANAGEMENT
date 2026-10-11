# Database & Data Assets Directory

This folder contains the complete SQL schemas, mock datasets, and database seeding scripts for the **Apex Bank Management System**.

---

## Files Overview

| File | Purpose |
|---|---|
| [`schema.sql`](file:///c:/Users/nanne/Desktop/bank%20management/data/schema.sql) | DDL script defining tables (`customers`, `users`, `accounts`, `transactions`, `transfers`), foreign keys, constraints, and indexes. |
| [`data.sql`](file:///c:/Users/nanne/Desktop/bank%20management/data/data.sql) | Seed SQL script inserting customers, staff, accounts, realistic transactions, and transfers. |
| [`mock-data.json`](file:///c:/Users/nanne/Desktop/bank%20management/data/mock-data.json) | Complete JSON format dataset for frontend testing, prototyping, or offline mock usage. |

---

## How to Import into MySQL

### 1. Using MySQL Command Line
```bash
# 1. Login and create database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bankdb;"

# 2. Execute schema DDL
mysql -u root -p bankdb < data/schema.sql

# 3. Execute seed data
mysql -u root -p bankdb < data/data.sql
```

### 2. Using MySQL Workbench or phpMyAdmin
1. Open MySQL Workbench and connect to your local MySQL instance.
2. Open `schema.sql` and execute it (creates the tables).
3. Open `data.sql` and execute it (populates the records).

---

## Default User Passwords

All demo passwords in `data.sql` are BCrypt hashed:

- **admin**: `Admin@123` (Role: ADMIN)
- **employee1**: `Emp@123` (Role: EMPLOYEE)
- **cust1**: `Cust@123` (Role: CUSTOMER - J. Abhilash)
- **cust2**: `Cust@123` (Role: CUSTOMER - Sarah Jenkins)
- **cust3**: `Cust@123` (Role: CUSTOMER - Michael Chen)
