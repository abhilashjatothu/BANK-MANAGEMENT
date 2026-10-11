package com.bankms.service;

import com.bankms.dto.CustomerForm;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.User;
import com.bankms.model.enums.Role;
import com.bankms.repository.CustomerRepository;
import com.bankms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service managing customer records and associated user credentials.
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new customer and optional user login credentials.
     *
     * @param form customer creation form
     * @return persisted customer
     */
    @Transactional
    public Customer createCustomer(CustomerForm form) {
        if (customerRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("A customer with email " + form.getEmail() + " already exists.");
        }

        if (form.getUsername() != null && !form.getUsername().trim().isEmpty()) {
            if (userRepository.existsByUsername(form.getUsername())) {
                throw new IllegalArgumentException("Username " + form.getUsername() + " is already in use.");
            }
        }

        Customer customer = new Customer(
                form.getName().trim(),
                form.getEmail().trim(),
                form.getPhone().trim(),
                form.getAddress() != null ? form.getAddress().trim() : ""
        );

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Created new customer id: {}, email: {}", savedCustomer.getCustomerId(), savedCustomer.getEmail());

        if (form.getUsername() != null && !form.getUsername().trim().isEmpty() &&
            form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            User user = new User(
                    form.getUsername().trim(),
                    passwordEncoder.encode(form.getPassword()),
                    Role.CUSTOMER,
                    true,
                    savedCustomer
            );
            userRepository.save(user);
            savedCustomer.setUser(user);
            log.info("Created linked customer user login: {}", user.getUsername());
        }

        return savedCustomer;
    }

    /**
     * Updates an existing customer profile.
     *
     * @param customerId id of the customer to update
     * @param form customer form containing updated details
     * @return updated customer
     */
    @Transactional
    public Customer updateCustomer(Long customerId, CustomerForm form) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));

        // Check email uniqueness if email changed
        if (!customer.getEmail().equalsIgnoreCase(form.getEmail().trim()) &&
            customerRepository.existsByEmail(form.getEmail().trim())) {
            throw new IllegalArgumentException("A customer with email " + form.getEmail() + " already exists.");
        }

        customer.setName(form.getName().trim());
        customer.setEmail(form.getEmail().trim());
        customer.setPhone(form.getPhone().trim());
        customer.setAddress(form.getAddress() != null ? form.getAddress().trim() : "");

        // If password is provided, update password for linked user
        if (customer.getUser() != null && form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            customer.getUser().setPasswordHash(passwordEncoder.encode(form.getPassword()));
            userRepository.save(customer.getUser());
            log.info("Updated password for customer user: {}", customer.getUser().getUsername());
        }

        return customerRepository.save(customer);
    }

    /**
     * Finds a customer by ID.
     *
     * @param customerId customer id
     * @return optional customer
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long customerId) {
        return customerRepository.findById(customerId);
    }

    /**
     * Finds a customer by email.
     *
     * @param email customer email
     * @return optional customer
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    /**
     * Returns a paginated list of customers with optional keyword filter.
     *
     * @param keyword search term
     * @param page zero-based page number
     * @param size page size
     * @return page of customers
     */
    @Transactional(readOnly = true)
    public Page<Customer> getCustomers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("customerId").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return customerRepository.searchCustomers(keyword.trim(), pageable);
        }
        return customerRepository.findAll(pageable);
    }

    /**
     * Returns all customers.
     *
     * @return list of all customers
     */
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    /**
     * Deletes a customer if no active accounts or positive balance exist.
     *
     * @param customerId id of customer to delete
     */
    @Transactional
    public void deleteCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));

        boolean hasNonZeroBalance = customer.getAccounts().stream()
                .anyMatch(a -> a.getBalance().compareTo(BigDecimal.ZERO) > 0);

        if (hasNonZeroBalance) {
            throw new IllegalStateException("Cannot delete customer with active accounts containing non-zero balance.");
        }

        log.info("Deleting customer id: {}", customerId);
        customerRepository.delete(customer);
    }

    /**
     * Gets total customer count.
     *
     * @return count of customers
     */
    @Transactional(readOnly = true)
    public long count() {
        return customerRepository.count();
    }
}
