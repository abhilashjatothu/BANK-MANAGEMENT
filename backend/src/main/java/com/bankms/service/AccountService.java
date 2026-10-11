package com.bankms.service;

import com.bankms.dto.AccountForm;
import com.bankms.exception.AccountNotFoundException;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.Transaction;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.TransactionStatus;
import com.bankms.model.enums.TransactionType;
import com.bankms.repository.AccountRepository;
import com.bankms.repository.CustomerRepository;
import com.bankms.repository.TransactionRepository;
import com.bankms.util.ReferenceNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing bank accounts, lifecycle status, and customer account ownership verification.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Opens a new bank account for a given customer.
     *
     * @param form account creation form
     * @return newly opened account
     */
    @Transactional
    public Account openAccount(AccountForm form) {
        Customer customer = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + form.getCustomerId()));

        String accountNumber;
        do {
            accountNumber = ReferenceNumberGenerator.generateAccountNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        BigDecimal initialDeposit = form.getInitialDeposit() != null ? form.getInitialDeposit() : BigDecimal.ZERO;
        AccountStatus initialStatus = form.getStatus() != null ? form.getStatus() : AccountStatus.ACTIVE;

        Account account = new Account(accountNumber, customer, form.getAccountType(), initialDeposit, initialStatus);
        Account savedAccount = accountRepository.save(account);
        log.info("Opened account {} for customer id: {} with balance: {}", accountNumber, customer.getCustomerId(), initialDeposit);

        // Record initial deposit transaction if initial deposit > 0
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
                    "Initial account opening deposit"
            );
            transactionRepository.save(transaction);
        }

        return savedAccount;
    }

    /**
     * Finds an account by account number.
     *
     * @param accountNumber 12-digit account number
     * @return account entity
     */
    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
    }

    /**
     * Finds an account by ID.
     *
     * @param accountId account id
     * @return account entity
     */
    @Transactional(readOnly = true)
    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
    }

    /**
     * Retrieves all accounts belonging to a customer.
     *
     * @param customerId customer id
     * @return list of accounts
     */
    @Transactional(readOnly = true)
    public List<Account> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByCustomerCustomerId(customerId);
    }

    /**
     * Retrieves a paginated list of accounts for a customer.
     *
     * @param customerId customer id
     * @param page page number
     * @param size page size
     * @return page of accounts
     */
    @Transactional(readOnly = true)
    public Page<Account> getAccountsByCustomerId(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        return accountRepository.findByCustomerCustomerId(customerId, pageable);
    }

    /**
     * Retrieves all accounts with pagination and optional search keyword.
     *
     * @param keyword search term
     * @param page page number
     * @param size page size
     * @return page of accounts
     */
    @Transactional(readOnly = true)
    public Page<Account> getAccounts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("accountId").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return accountRepository.searchAccounts(keyword.trim(), pageable);
        }
        return accountRepository.findAll(pageable);
    }

    /**
     * Updates an account's operational status.
     *
     * @param accountNumber 12-digit account number
     * @param newStatus ACTIVE, FROZEN, or CLOSED
     * @return updated account
     */
    @Transactional
    public Account updateAccountStatus(String accountNumber, AccountStatus newStatus) {
        Account account = getAccountByNumber(accountNumber);

        if (newStatus == AccountStatus.CLOSED && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account balance must be 0.00 before closing. Current balance is: " + account.getBalance());
        }

        account.setStatus(newStatus);
        log.info("Updated status of account {} to {}", accountNumber, newStatus);
        return accountRepository.save(account);
    }

    /**
     * Verifies that the specified customer owns the account.
     * Throws AccessDeniedException if ownership check fails.
     *
     * @param accountNumber account number
     * @param customerId expected customer id
     */
    @Transactional(readOnly = true)
    public void validateCustomerOwnsAccount(String accountNumber, Long customerId) {
        if (customerId == null) {
            throw new AccessDeniedException("Unauthorized: No customer profile associated with current session.");
        }
        Account account = getAccountByNumber(accountNumber);
        if (!account.getCustomer().getCustomerId().equals(customerId)) {
            log.warn("Security Alert: Customer id {} attempted unauthorized access to account {}", customerId, accountNumber);
            throw new AccessDeniedException("Access denied: You do not own this account.");
        }
    }

    /**
     * Calculates the sum of all account balances in the bank.
     *
     * @return total bank balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalBankBalance() {
        return accountRepository.sumTotalBalance();
    }

    /**
     * Counts total number of accounts.
     *
     * @return total count
     */
    @Transactional(readOnly = true)
    public long count() {
        return accountRepository.count();
    }

    /**
     * Counts accounts by their status.
     *
     * @param status account status
     * @return count
     */
    @Transactional(readOnly = true)
    public long countByStatus(AccountStatus status) {
        return accountRepository.countByStatus(status);
    }
}
