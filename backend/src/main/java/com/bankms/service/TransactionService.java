package com.bankms.service;

import com.bankms.dto.DepositForm;
import com.bankms.dto.TransferForm;
import com.bankms.dto.WithdrawForm;
import com.bankms.exception.AccountNotFoundException;
import com.bankms.exception.InsufficientBalanceException;
import com.bankms.exception.InvalidTransactionException;
import com.bankms.model.Account;
import com.bankms.model.Transaction;
import com.bankms.model.Transfer;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.TransactionStatus;
import com.bankms.model.enums.TransactionType;
import com.bankms.repository.AccountRepository;
import com.bankms.repository.TransactionRepository;
import com.bankms.repository.TransferRepository;
import com.bankms.util.ReferenceNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing financial transactions including deposits, withdrawals, fund transfers,
 * and transaction history with concurrency control and deadlock prevention.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
    }

    /**
     * Executes a deposit into an active account.
     *
     * @param form deposit form
     * @return created transaction record
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction deposit(DepositForm form) {
        validateAmount(form.getAmount());

        Account account = accountRepository.findByAccountNumberWithLock(form.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + form.getAccountNumber()));

        validateAccountIsActive(account);

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal newBalance = balanceBefore.add(form.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        String referenceNo = ReferenceNumberGenerator.generateTransactionReference();
        String desc = (form.getDescription() != null && !form.getDescription().trim().isEmpty())
                ? form.getDescription().trim()
                : "Cash Deposit";

        Transaction transaction = new Transaction(
                account,
                TransactionType.DEPOSIT,
                form.getAmount(),
                newBalance,
                LocalDateTime.now(),
                TransactionStatus.SUCCESS,
                referenceNo,
                desc
        );

        Transaction savedTx = transactionRepository.save(transaction);
        log.info("Deposit successful: {} credited to account {}. New balance: {}. Ref: {}",
                form.getAmount(), account.getAccountNumber(), newBalance, referenceNo);
        return savedTx;
    }

    /**
     * Executes a withdrawal from an active account with sufficient balance.
     *
     * @param form withdrawal form
     * @return created transaction record
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction withdraw(WithdrawForm form) {
        validateAmount(form.getAmount());

        Account account = accountRepository.findByAccountNumberWithLock(form.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + form.getAccountNumber()));

        validateAccountIsActive(account);

        if (account.getBalance().compareTo(form.getAmount()) < 0) {
            log.warn("Withdrawal failed for account {}: requested {}, available {}",
                    account.getAccountNumber(), form.getAmount(), account.getBalance());
            throw new InsufficientBalanceException("Insufficient funds. Available balance: " + account.getBalance());
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal newBalance = balanceBefore.subtract(form.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        String referenceNo = ReferenceNumberGenerator.generateTransactionReference();
        String desc = (form.getDescription() != null && !form.getDescription().trim().isEmpty())
                ? form.getDescription().trim()
                : "Cash Withdrawal";

        Transaction transaction = new Transaction(
                account,
                TransactionType.WITHDRAWAL,
                form.getAmount(),
                newBalance,
                LocalDateTime.now(),
                TransactionStatus.SUCCESS,
                referenceNo,
                desc
        );

        Transaction savedTx = transactionRepository.save(transaction);
        log.info("Withdrawal successful: {} debited from account {}. New balance: {}. Ref: {}",
                form.getAmount(), account.getAccountNumber(), newBalance, referenceNo);
        return savedTx;
    }

    /**
     * Transfers funds between two active bank accounts atomically.
     * To prevent deadlocks, accounts are acquired and locked in ascending account ID order.
     *
     * @param form fund transfer form
     * @return created transfer record
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transfer transfer(TransferForm form) {
        validateAmount(form.getAmount());

        String fromAccNum = form.getFromAccountNumber().trim();
        String toAccNum = form.getToAccountNumber().trim();

        if (fromAccNum.equalsIgnoreCase(toAccNum)) {
            throw new InvalidTransactionException("Source and destination accounts cannot be the same.");
        }

        // Pre-lookup to determine ID ordering for deadlock prevention
        Account sourceBasic = accountRepository.findByAccountNumber(fromAccNum)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + fromAccNum));
        Account targetBasic = accountRepository.findByAccountNumber(toAccNum)
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + toAccNum));

        Account fromAccount;
        Account toAccount;

        // Deadlock Prevention: Always acquire locks in ascending accountId order
        if (sourceBasic.getAccountId() < targetBasic.getAccountId()) {
            fromAccount = accountRepository.findByIdWithLock(sourceBasic.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + fromAccNum));
            toAccount = accountRepository.findByIdWithLock(targetBasic.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + toAccNum));
        } else {
            toAccount = accountRepository.findByIdWithLock(targetBasic.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + toAccNum));
            fromAccount = accountRepository.findByIdWithLock(sourceBasic.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + fromAccNum));
        }

        validateAccountIsActive(fromAccount);
        validateAccountIsActive(toAccount);

        if (fromAccount.getBalance().compareTo(form.getAmount()) < 0) {
            log.warn("Transfer failed: Account {} has insufficient funds. Available: {}, Requested: {}",
                    fromAccount.getAccountNumber(), fromAccount.getBalance(), form.getAmount());
            throw new InsufficientBalanceException("Insufficient funds in source account. Available balance: " + fromAccount.getBalance());
        }

        BigDecimal sourceNewBalance = fromAccount.getBalance().subtract(form.getAmount());
        BigDecimal targetNewBalance = toAccount.getBalance().add(form.getAmount());

        fromAccount.setBalance(sourceNewBalance);
        toAccount.setBalance(targetNewBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String referenceNo = ReferenceNumberGenerator.generateTransferReference();
        LocalDateTime now = LocalDateTime.now();

        // 1. Create Transfer record
        Transfer transfer = new Transfer(fromAccount, toAccount, form.getAmount(), now, TransactionStatus.SUCCESS, referenceNo);
        Transfer savedTransfer = transferRepository.save(transfer);

        // 2. Create TRANSFER_DEBIT transaction on source account
        String debitDesc = "Transfer to " + toAccount.getAccountNumber() +
                (form.getDescription() != null && !form.getDescription().trim().isEmpty() ? " - " + form.getDescription().trim() : "");
        Transaction debitTx = new Transaction(
                fromAccount,
                TransactionType.TRANSFER_DEBIT,
                form.getAmount(),
                sourceNewBalance,
                now,
                TransactionStatus.SUCCESS,
                referenceNo,
                debitDesc
        );
        transactionRepository.save(debitTx);

        // 3. Create TRANSFER_CREDIT transaction on destination account
        String creditDesc = "Transfer from " + fromAccount.getAccountNumber() +
                (form.getDescription() != null && !form.getDescription().trim().isEmpty() ? " - " + form.getDescription().trim() : "");
        Transaction creditTx = new Transaction(
                toAccount,
                TransactionType.TRANSFER_CREDIT,
                form.getAmount(),
                targetNewBalance,
                now,
                TransactionStatus.SUCCESS,
                referenceNo,
                creditDesc
        );
        transactionRepository.save(creditTx);

        log.info("Transfer successful: {} transferred from {} to {}. Ref: {}",
                form.getAmount(), fromAccount.getAccountNumber(), toAccount.getAccountNumber(), referenceNo);

        return savedTransfer;
    }

    /**
     * Retrieves paginated transaction history with optional filters.
     *
     * @param accountId optional account ID filter
     * @param type optional transaction type
     * @param startDate optional start date
     * @param endDate optional end date
     * @param page page number
     * @param size page size
     * @return page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getFilteredTransactions(Long accountId, TransactionType type,
                                                    LocalDateTime startDate, LocalDateTime endDate,
                                                    int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findFilteredTransactions(accountId, type, startDate, endDate, pageable);
    }

    /**
     * Retrieves paginated transaction history restricted to a specific customer.
     *
     * @param customerId customer ID
     * @param accountId optional account ID filter
     * @param type optional transaction type
     * @param startDate optional start date
     * @param endDate optional end date
     * @param page page number
     * @param size page size
     * @return page of transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getCustomerFilteredTransactions(Long customerId, Long accountId, TransactionType type,
                                                            LocalDateTime startDate, LocalDateTime endDate,
                                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findFilteredCustomerTransactions(customerId, accountId, type, startDate, endDate, pageable);
    }

    /**
     * Validates that transaction amount is strictly positive and has at most 2 decimal places.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionException("Amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be strictly greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new InvalidTransactionException("Amount cannot have more than 2 decimal places.");
        }
    }

    /**
     * Validates that account status is ACTIVE.
     */
    private void validateAccountIsActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Account " + account.getAccountNumber() +
                    " is " + account.getStatus() + ". Only ACTIVE accounts can process transactions.");
        }
    }
}
