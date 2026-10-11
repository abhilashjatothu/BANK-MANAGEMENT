# Bank Management System - Java Full Stack

A centralized, enterprise-grade Bank Management System built exclusively using Java technologies (**Spring Boot 3**, **Thymeleaf**, **Spring Security**, **Spring Data JPA**, **MySQL**, and **Bootstrap 5**).

---

## 1. Tech Stack (Strictly Java)

| Layer | Technology |
|---|---|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.2.x (Spring MVC) |
| **Frontend** | Thymeleaf + HTML5 / CSS3 (Bootstrap 5 & Icons via WebJars) |
| **Security** | Spring Security 6 (Session-based form login, BCrypt hashing, method-level `@PreAuthorize`) |
| **Persistence** | Spring Data JPA + Hibernate |
| **Database** | MySQL (Production/Local default), H2 in-memory (Tests & Dev profile) |
| **Build & Test** | Maven 3.9+, JUnit 5, Mockito, Spring Boot Test |

---

## 2. Package & File Structure

```text
bank-management-system/
|-- pom.xml
|-- README.md
|-- .gitignore
`-- src/
    |-- main/
    |   |-- java/com/bankms/
    |   |   |-- BankManagementApplication.java
    |   |   |-- config/
    |   |   |   |-- SecurityConfig.java          # SecurityFilterChain, RBAC redirect handler
    |   |   |   |-- WebConfig.java               # Static resource & view controllers
    |   |   |   `-- DataInitializer.java         # Seeds admin, employee, demo customer & accounts
    |   |   |-- controller/
    |   |   |   |-- AuthController.java          # Login, logout, dynamic dashboard router
    |   |   |   |-- DashboardController.java     # Admin, Employee, Customer dashboards
    |   |   |   |-- CustomerController.java      # Customer CRUD & profile management
    |   |   |   |-- AccountController.java       # Account opening, listing, status toggles
    |   |   |   |-- TransactionController.java   # Deposit, withdraw, transfer, history
    |   |   |   `-- ReportController.java        # Aggregate financial metrics & reports
    |   |   |-- service/
    |   |   |   |-- UserService.java             # User registration & lookup
    |   |   |   |-- CustomerService.java         # Customer entity lifecycle
    |   |   |   |-- AccountService.java          # 12-digit account generation, ownership checks
    |   |   |   |-- TransactionService.java      # @Transactional banking, pessimistic locking
    |   |   |   |-- ReportService.java           # Dashboard statistics & aggregations
    |   |   |   `-- CustomUserDetailsService.java# UserDetails & CustomUserDetails principal
    |   |   |-- repository/
    |   |   |   |-- UserRepository.java
    |   |   |   |-- CustomerRepository.java
    |   |   |   |-- AccountRepository.java       # Pessimistic locking queries & balance sums
    |   |   |   |-- TransactionRepository.java   # Filtered history & volume queries
    |   |   |   `-- TransferRepository.java
    |   |   |-- model/
    |   |   |   |-- User.java
    |   |   |   |-- Customer.java
    |   |   |   |-- Account.java                 # @Version optimistic locking
    |   |   |   |-- Transaction.java
    |   |   |   |-- Transfer.java
    |   |   |   `-- enums/
    |   |   |       |-- Role.java                # ADMIN, EMPLOYEE, CUSTOMER
    |   |   |       |-- AccountType.java         # SAVINGS, CURRENT
    |   |   |       |-- AccountStatus.java       # ACTIVE, FROZEN, CLOSED
    |   |   |       |-- TransactionType.java     # DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT
    |   |   |       `-- TransactionStatus.java   # SUCCESS, FAILED
    |   |   |-- dto/
    |   |   |   |-- CustomerForm.java
    |   |   |   |-- AccountForm.java
    |   |   |   |-- DepositForm.java
    |   |   |   |-- WithdrawForm.java
    |   |   |   |-- TransferForm.java
    |   |   |   `-- AccountSummaryDto.java
    |   |   |-- exception/
    |   |   |   |-- InsufficientBalanceException.java
    |   |   |   |-- AccountNotFoundException.java
    |   |   |   |-- InvalidTransactionException.java
    |   |   |   `-- GlobalExceptionHandler.java  # Custom error views & status mappings
    |   |   `-- util/
    |   |       `-- ReferenceNumberGenerator.java# Unique TXN/TRF reference numbers
    |   `-- resources/
    |       |-- application.properties           # MySQL config with env variable fallbacks
    |       |-- application-dev.properties       # Zero-config in-memory H2 profile
    |       |-- schema.sql                       # DDL schema definitions
    |       |-- static/
    |       |   |-- css/style.css                # Premium modern bank styling
    |       |   `-- js/app.js                    # Auto-dismiss alerts & submit helpers
    |       `-- templates/
    |           |-- layout/ (base.html, navbar.html, footer.html)
    |           |-- auth/login.html
    |           |-- dashboard/ (admin.html, employee.html, customer.html)
    |           |-- customer/ (list.html, form.html, view.html)
    |           |-- account/ (list.html, form.html, view.html)
    |           |-- transaction/ (deposit.html, withdraw.html, transfer.html, history.html)
    |           |-- report/summary.html
    |           `-- error/ (403.html, 404.html, error.html)
    `-- test/java/com/bankms/
        |-- service/
        |   |-- TransactionServiceTest.java      # Mockito unit tests for deposit, withdraw, transfer, locks
        |   `-- AccountServiceTest.java          # Ownership validation & account limits
        |-- controller/
        |   `-- TransactionControllerTest.java   # MockMvc RBAC & 403 access control tests
        `-- repository/
            `-- AccountRepositoryTest.java       # DataJpaTest with in-memory H2
```

---

## 3. Database Design & Relationships

- **`users`** (`user_id` PK, `username` UNIQUE, `password_hash`, `role`, `enabled`, `customer_id` FK NULLABLE)
- **`customers`** (`customer_id` PK, `name`, `email` UNIQUE, `phone`, `address`, `created_at`)
- **`accounts`** (`account_id` PK, `account_number` UNIQUE (12 digits), `customer_id` FK, `account_type`, `balance` DECIMAL(15,2), `status`, `created_at`, `@Version version`)
- **`transactions`** (`transaction_id` PK, `account_id` FK, `type`, `amount` DECIMAL(15,2), `balance_after` DECIMAL(15,2), `date_time`, `status`, `reference_no` UNIQUE, `description`)
- **`transfers`** (`transfer_id` PK, `from_account_id` FK, `to_account_id` FK, `amount` DECIMAL(15,2), `date_time`, `status`, `reference_no` UNIQUE)

### Key Relational Rules:
1. `User` 1:1 `Customer` (Optional; staff users ADMIN/EMPLOYEE have no customer reference).
2. `Customer` 1:N `Account`.
3. `Account` 1:N `Transaction`.
4. An inter-account transfer atomically creates **one `Transfer` row** and **two `Transaction` rows** (`TRANSFER_DEBIT` on sender, `TRANSFER_CREDIT` on recipient) sharing the exact same `reference_no`.
5. `BigDecimal` is strictly used for all financial calculations; floats and doubles are prohibited.

---

## 4. Concurrency Control & Deadlock Prevention

- **Pessimistic Write Locking**: Balance-modifying operations acquire `@Lock(LockModeType.PESSIMISTIC_WRITE)` when retrieving accounts.
- **Deadlock Prevention in Transfers**: To eliminate database deadlocks when two users transfer funds between the same accounts simultaneously (A &rarr; B and B &rarr; A), the `TransactionService` compares account IDs and **always acquires locks in ascending `account_id` order**.
- **Optimistic Locking**: `@Version` field on `Account` prevents lost updates during concurrent edits.
- **Strict Business Validations**:
  - Rejection of zero, negative, or >2 decimal place amounts.
  - Rejection of overdraft attempts (insufficient balance).
  - Rejection of transactions on `FROZEN` or `CLOSED` accounts.
  - Rejection of self-transfers (source == destination).

---

## 5. Prerequisites & Database Setup

### Prerequisites
- **JDK 17+** (JDK 17, 21, or 27)
- **Maven 3.8+**
- **MySQL 8.0+** (Optional if using the `dev` H2 profile)

### MySQL Setup
In MySQL client or workbench:
```sql
CREATE DATABASE IF NOT EXISTS bankdb;
```

If your MySQL user is not `root` or uses a custom password, set environment variables or pass them at launch:
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bankdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
```

---

## 6. How to Run the Application

### Option A: Run with MySQL (Production/Default)
```bash
mvn spring-boot:run
```

### Option B: Run with Zero-Config In-Memory H2 (`dev` profile)
If MySQL is not installed or running, launch with the `dev` profile to use an embedded in-memory database:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Open your browser to: **[http://localhost:8080](http://localhost:8080)**

### Running Automated Test Suite
```bash
mvn test
```

### Building Executable JAR
```bash
mvn clean install
java -jar target/bank-management-system-1.0.0.jar
```

---

## 7. Default Seeded Logins

The application automatically seeds initial users and accounts on first start if the database is empty:

| Role | Username | Password | Default Landing Dashboard | Notes |
|---|---|---|---|---|
| **ADMIN** | `admin` | `Admin@123` | `/dashboard/admin` | Full system access, staff, reports, customers, accounts |
| **EMPLOYEE** | `employee1` | `Emp@123` | `/dashboard/employee` | Branch desk, customer onboarding, deposits, withdrawals, transfers |
| **CUSTOMER** | `cust1` | `Cust@123` | `/dashboard/customer` | Customer portal: seeded with 1 Savings ($5,000) & 1 Current ($12,500) account |

---

## 8. Role-Based Permissions Summary

- **ADMIN**:
  - Full access to all modules, system metrics, reports, customer records, and accounts.
  - Can delete customers (when account balances are $0.00).
- **EMPLOYEE**:
  - Register new customers and open new accounts.
  - Perform cash deposits, withdrawals, and inter-account transfers on behalf of customers.
  - View financial reports and audit logs.
- **CUSTOMER**:
  - Access only their own accounts and profile.
  - Perform self-service deposits, withdrawals, and transfers from their own accounts.
  - View paginated statement history for their accounts.
  - **Ownership Isolation**: Any attempt to query or transact on another customer's account is strictly intercepted and blocked with HTTP 403 Forbidden.

---

## 9. Screenshot & Walkthrough Guide

1. **Sign In (`/login`)**:
   - Modern split-card sign-in screen with quick-fill demo credentials reference and feedback alerts.
2. **Administrator Dashboard (`/dashboard/admin`)**:
   - High-level KPI stat cards (Total Customers, Total Accounts, Total Bank Vault Balance, Today's Transaction Volume).
   - Account status distribution (Active, Frozen, Closed) and classification (Savings vs Current).
   - Real-time audit log of the most recent transactions.
3. **Staff / Employee Dashboard (`/dashboard/employee`)**:
   - Quick-action banking operation shortcuts (Cash Deposit, Cash Withdrawal, Wire Transfer).
   - Today's volume and operational metrics.
4. **Customer Portal (`/dashboard/customer`)**:
   - Interactive account cards displaying 12-digit account numbers, status badges, and formatted available balances.
   - Quick-action buttons to directly initiate deposit, withdraw, or transfer for that specific account.
5. **Customer Management (`/customers`)**:
   - Paginated customer directory with real-time keyword search (name, email, phone).
   - Add/Edit forms with server-side validation error tooltips.
6. **Account Statement View (`/accounts/view/{accountNumber}`)**:
   - Detailed balance statement, customer holder link, status toggle (Active / Frozen / Closed), and paginated transaction history.
7. **Deposit & Withdrawal (`/transactions/deposit`, `/transactions/withdraw`)**:
   - Form controls with account selector and double-submit prevention.
8. **Fund Transfer (`/transactions/transfer`)**:
   - Source account debit and destination account credit executed inside a single `@Transactional` boundary with deadlock prevention.
9. **Transaction Statement Logs (`/transactions/history`)**:
   - Multi-criteria filterable history table with account dropdown, transaction type filter, and date-range picker.
10. **Reports & Analytics (`/reports/summary`)**:
    - Comprehensive vault breakdown, transaction type distributions (DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT), and printable report view.
11. **Security & Access Control (`/error/403`)**:
    - Custom styled 403 Forbidden page displayed whenever unauthorized access or customer cross-access is detected.
