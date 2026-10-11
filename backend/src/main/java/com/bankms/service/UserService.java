package com.bankms.service;

import com.bankms.dto.ForgotPasswordForm;
import com.bankms.dto.RegisterForm;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.Transaction;
import com.bankms.model.User;
import com.bankms.model.enums.AccountStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user credentials, roles, and authentication identities.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
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

    /**
     * Registers a new user.
     *
     * @param username raw username
     * @param rawPassword plain text password
     * @param role user role (ADMIN, EMPLOYEE, CUSTOMER)
     * @param customer linked customer or null for staff
     * @return persisted user
     */
    @Transactional
    public User registerUser(String username, String rawPassword, Role role, Customer customer) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken");
        }
        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = new User(username, passwordHash, role, true, customer);
        log.info("Registering user: {} with role: {}", username, role);
        return userRepository.save(user);
    }

    /**
     * Finds a user by their username.
     *
     * @param username username to search
     * @return optional user
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Finds a user by ID.
     *
     * @param id user id
     * @return optional user
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Lists all users in the system.
     *
     * @return list of users
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Registers a new customer along with credentials and an initial bank account.
     *
     * @param form registration details
     * @return registered User entity
     */
    @Transactional
    public User registerCustomerWithAccount(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String username = form.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken");
        }

        String email = form.getEmail().trim();
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A customer with email '" + email + "' already exists");
        }

        Customer customer = new Customer(
                form.getName().trim(),
                email,
                form.getPhone().trim(),
                form.getAddress() != null ? form.getAddress().trim() : ""
        );
        Customer savedCustomer = customerRepository.save(customer);

        String passwordHash = passwordEncoder.encode(form.getPassword());
        User user = new User(username, passwordHash, Role.CUSTOMER, true, savedCustomer);
        User savedUser = userRepository.save(user);
        savedCustomer.setUser(savedUser);

        // Generate unique account number
        String accountNumber;
        do {
            accountNumber = ReferenceNumberGenerator.generateAccountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        BigDecimal initialDeposit = form.getInitialDeposit() != null ? form.getInitialDeposit() : BigDecimal.ZERO;
        Account account = new Account(accountNumber, savedCustomer, form.getAccountType(), initialDeposit, AccountStatus.ACTIVE);
        Account savedAccount = accountRepository.save(account);

        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            String refNo = ReferenceNumberGenerator.generateTransactionReference();
            Transaction transaction = new Transaction(
                    savedAccount,
                    TransactionType.DEPOSIT,
                    initialDeposit,
                    initialDeposit,
                    LocalDateTime.now(),
                    TransactionStatus.SUCCESS,
                    refNo,
                    "Initial opening balance deposit"
            );
            transactionRepository.save(transaction);
        }

        log.info("Successfully registered customer {} with username {} and account {}",
                savedCustomer.getName(), username, accountNumber);

        return savedUser;
    }

    /**
     * Verifies identity and resets password for an existing user.
     *
     * @param form forgot password details
     */
    @Transactional
    public void resetPassword(ForgotPasswordForm form) {
        if (!form.getNewPassword().equals(form.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        String username = form.getUsername().trim();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No account found with username '" + username + "'"));

        // Verify email match with linked customer
        if (user.getCustomer() == null ||
            !user.getCustomer().getEmail().equalsIgnoreCase(form.getEmail().trim())) {
            throw new IllegalArgumentException("Username and email combination do not match our records.");
        }

        String newHash = passwordEncoder.encode(form.getNewPassword());
        user.setPasswordHash(newHash);
        userRepository.save(user);
        log.info("Password successfully reset for user: {}", username);
    }
}
