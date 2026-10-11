package com.bankms.dto;

import java.math.BigDecimal;

public class AccountSummaryDto {

    private long totalCustomers;
    private long totalAccounts;
    private long activeAccounts;
    private long frozenAccounts;
    private long closedAccounts;
    private BigDecimal totalBalance;
    private long todayTransactionCount;
    private BigDecimal todayTransactionVolume;
    private long savingsAccountsCount;
    private long currentAccountsCount;

    public AccountSummaryDto() {
        this.totalBalance = BigDecimal.ZERO;
        this.todayTransactionVolume = BigDecimal.ZERO;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public long getActiveAccounts() {
        return activeAccounts;
    }

    public void setActiveAccounts(long activeAccounts) {
        this.activeAccounts = activeAccounts;
    }

    public long getFrozenAccounts() {
        return frozenAccounts;
    }

    public void setFrozenAccounts(long frozenAccounts) {
        this.frozenAccounts = frozenAccounts;
    }

    public long getClosedAccounts() {
        return closedAccounts;
    }

    public void setClosedAccounts(long closedAccounts) {
        this.closedAccounts = closedAccounts;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public long getTodayTransactionCount() {
        return todayTransactionCount;
    }

    public void setTodayTransactionCount(long todayTransactionCount) {
        this.todayTransactionCount = todayTransactionCount;
    }

    public BigDecimal getTodayTransactionVolume() {
        return todayTransactionVolume;
    }

    public void setTodayTransactionVolume(BigDecimal todayTransactionVolume) {
        this.todayTransactionVolume = todayTransactionVolume;
    }

    public long getSavingsAccountsCount() {
        return savingsAccountsCount;
    }

    public void setSavingsAccountsCount(long savingsAccountsCount) {
        this.savingsAccountsCount = savingsAccountsCount;
    }

    public long getCurrentAccountsCount() {
        return currentAccountsCount;
    }

    public void setCurrentAccountsCount(long currentAccountsCount) {
        this.currentAccountsCount = currentAccountsCount;
    }
}
