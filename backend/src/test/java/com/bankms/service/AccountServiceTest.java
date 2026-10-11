package com.bankms.service;

import com.bankms.dto.AccountForm;
import com.bankms.exception.AccountNotFoundException;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.AccountType;
import com.bankms.repository.AccountRepository;
import com.bankms.repository.CustomerRepository;
import com.bankms.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Customer customer1;
    private Customer customer2;
    private Account account;

    @BeforeEach
    void setUp() {
        customer1 = new Customer("Customer 1", "cust1@test.com", "1234567890", "Address 1");
        customer1.setCustomerId(100L);

        customer2 = new Customer("Customer 2", "cust2@test.com", "0987654321", "Address 2");
        customer2.setCustomerId(200L);

        account = new Account("101010101010", customer1, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE);
        account.setAccountId(1L);
    }

    @Test
    @DisplayName("Open Account: Generates 12-digit account number and persists account")
    void testOpenAccount() {
        AccountForm form = new AccountForm();
        form.setCustomerId(100L);
        form.setAccountType(AccountType.CURRENT);
        form.setInitialDeposit(new BigDecimal("100.00"));
        form.setStatus(AccountStatus.ACTIVE);

        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer1));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account opened = accountService.openAccount(form);

        assertNotNull(opened);
        assertNotNull(opened.getAccountNumber());
        assertEquals(12, opened.getAccountNumber().length());
        assertEquals(new BigDecimal("100.00"), opened.getBalance());
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("Security Check: Customer owns account validates successfully")
    void testValidateCustomerOwnsAccountSuccess() {
        when(accountRepository.findByAccountNumber("101010101010")).thenReturn(Optional.of(account));

        assertDoesNotThrow(() -> accountService.validateCustomerOwnsAccount("101010101010", 100L));
    }

    @Test
    @DisplayName("Security Check: Customer does not own account throws AccessDeniedException")
    void testValidateCustomerOwnsAccountForbidden() {
        when(accountRepository.findByAccountNumber("101010101010")).thenReturn(Optional.of(account));

        assertThrows(AccessDeniedException.class, () ->
                accountService.validateCustomerOwnsAccount("101010101010", 200L)
        );
    }

    @Test
    @DisplayName("Status Update: Cannot close account if balance is non-zero")
    void testCannotCloseAccountWithBalance() {
        when(accountRepository.findByAccountNumber("101010101010")).thenReturn(Optional.of(account));

        assertThrows(IllegalStateException.class, () ->
                accountService.updateAccountStatus("101010101010", AccountStatus.CLOSED)
        );
    }
}
