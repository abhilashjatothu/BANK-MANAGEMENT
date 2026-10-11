package com.bankms.config;

import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.Transaction;
import com.bankms.model.User;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.AccountType;
import com.bankms.model.enums.Role;
import com.bankms.model.enums.TransactionStatus;
import com.bankms.model.enums.TransactionType;
import com.bankms.repository.AccountRepository;
import com.bankms.repository.CustomerRepository;
import com.bankms.repository.TransactionRepository;
import com.bankms.repository.UserRepository;
import com.bankms.util.ReferenceNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Initializes default user roles, demo customer, accounts, and sample transactions if database is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CustomerRepository customerRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users. Skipping DataInitializer seeding.");
            return;
        }

        log.info("Seeding initial data into database...");

        // 1. Seed Admin
        User admin = new User(
                "admin",
                passwordEncoder.encode("Admin@123"),
                Role.ADMIN,
                true,
                null
        );
        userRepository.save(admin);
        log.info("Seeded Admin user: admin / Admin@123");

        // 2. Seed Employee
        User employee = new User(
                "employee1",
                passwordEncoder.encode("Emp@123"),
                Role.EMPLOYEE,
                true,
                null
        );
        userRepository.save(employee);
        log.info("Seeded Employee user: employee1 / Emp@123");

        // 3. Seed Demo Customer
        Customer customer = new Customer(
                "J. Abhilash",
                "abhilash@bankms.com",
                "+1-555-0199",
                "100 Financial Way, Tech Hub"
        );
        Customer savedCustomer = customerRepository.save(customer);

        User customerUser = new User(
                "cust1",
                passwordEncoder.encode("Cust@123"),
                Role.CUSTOMER,
                true,
                savedCustomer
        );
        userRepository.save(customerUser);
        savedCustomer.setUser(customerUser);
        log.info("Seeded Demo Customer user: cust1 / Cust@123");

        // 4. Seed 2 Accounts for Demo Customer
        Account savingsAccount = new Account(
                "101020304050",
                savedCustomer,
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                AccountStatus.ACTIVE
        );
        savingsAccount = accountRepository.saveAndFlush(savingsAccount);

        Account currentAccount = new Account(
                "101090807060",
                savedCustomer,
                AccountType.CURRENT,
                new BigDecimal("12500.00"),
                AccountStatus.ACTIVE
        );
        currentAccount = accountRepository.saveAndFlush(currentAccount);

        // 5. Seed Initial Transaction Records
        Transaction t1 = new Transaction(
                savingsAccount,
                TransactionType.DEPOSIT,
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00"),
                LocalDateTime.now().minusDays(3),
                TransactionStatus.SUCCESS,
                ReferenceNumberGenerator.generateTransactionReference(),
                "Initial account opening deposit"
        );
        transactionRepository.save(t1);

        Transaction t2 = new Transaction(
                currentAccount,
                TransactionType.DEPOSIT,
                new BigDecimal("15000.00"),
                new BigDecimal("15000.00"),
                LocalDateTime.now().minusDays(2),
                TransactionStatus.SUCCESS,
                ReferenceNumberGenerator.generateTransactionReference(),
                "Initial corporate funding"
        );
        transactionRepository.save(t2);

        Transaction t3 = new Transaction(
                currentAccount,
                TransactionType.WITHDRAWAL,
                new BigDecimal("2500.00"),
                new BigDecimal("12500.00"),
                LocalDateTime.now().minusDays(1),
                TransactionStatus.SUCCESS,
                ReferenceNumberGenerator.generateTransactionReference(),
                "ATM cash withdrawal"
        );
        transactionRepository.save(t3);

        log.info("Database seeding completed successfully.");
    }
}
