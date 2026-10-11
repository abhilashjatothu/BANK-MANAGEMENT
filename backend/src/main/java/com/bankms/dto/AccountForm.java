package com.bankms.dto;

import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AccountForm {

    @NotNull(message = "Customer is required")
    private Long customerId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Initial deposit amount is required")
    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    private AccountStatus status = AccountStatus.ACTIVE;

    public AccountForm() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
