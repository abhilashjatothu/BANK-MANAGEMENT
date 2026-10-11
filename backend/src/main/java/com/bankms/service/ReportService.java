package com.bankms.service;

import com.bankms.dto.AccountSummaryDto;
import com.bankms.model.Account;
import com.bankms.model.Transaction;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.AccountType;
import com.bankms.repository.AccountRepository;
import com.bankms.repository.CustomerRepository;
import com.bankms.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service providing statistical reports and aggregate summaries for dashboards.
 */
@Service
public class ReportService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public ReportService(CustomerRepository customerRepository,
                         AccountRepository accountRepository,
                         TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Generates comprehensive system-wide summary metrics.
     *
     * @return account summary metrics DTO
     */
    @Transactional(readOnly = true)
    public AccountSummaryDto getSystemSummary() {
        AccountSummaryDto summary = new AccountSummaryDto();

        summary.setTotalCustomers(customerRepository.count());
        summary.setTotalAccounts(accountRepository.count());
        summary.setActiveAccounts(accountRepository.countByStatus(AccountStatus.ACTIVE));
        summary.setFrozenAccounts(accountRepository.countByStatus(AccountStatus.FROZEN));
        summary.setClosedAccounts(accountRepository.countByStatus(AccountStatus.CLOSED));

        BigDecimal totalBal = accountRepository.sumTotalBalance();
        summary.setTotalBalance(totalBal != null ? totalBal : BigDecimal.ZERO);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        summary.setTodayTransactionCount(transactionRepository.countTransactionsByDateRange(startOfDay, endOfDay));
        BigDecimal volume = transactionRepository.sumVolumeByDateRange(startOfDay, endOfDay);
        summary.setTodayTransactionVolume(volume != null ? volume : BigDecimal.ZERO);

        List<Account> allAccounts = accountRepository.findAll();
        long savings = allAccounts.stream().filter(a -> a.getAccountType() == AccountType.SAVINGS).count();
        long current = allAccounts.stream().filter(a -> a.getAccountType() == AccountType.CURRENT).count();
        summary.setSavingsAccountsCount(savings);
        summary.setCurrentAccountsCount(current);

        return summary;
    }

    /**
     * Retrieves aggregated transaction counts and amounts grouped by transaction type.
     *
     * @return list of type summary maps
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTransactionTotalsPerType() {
        List<Object[]> rawList = transactionRepository.findTransactionTotalsPerType();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Object[] row : rawList) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", row[0].toString());
            map.put("count", row[1]);
            map.put("totalAmount", row[2]);
            results.add(map);
        }
        return results;
    }

    /**
     * Retrieves recent transactions across all accounts.
     *
     * @return list of recent transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> getRecentTransactions() {
        return transactionRepository.findTop5ByOrderByDateTimeDesc();
    }

    /**
     * Retrieves recent transactions for a specific customer.
     *
     * @param customerId customer ID
     * @return list of customer's recent transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> getRecentTransactionsForCustomer(Long customerId) {
        return transactionRepository.findTop5ByCustomer(customerId, PageRequest.of(0, 5));
    }
}
