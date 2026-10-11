package com.bankms.service;

import com.bankms.dto.DepositForm;
import com.bankms.dto.TransferForm;
import com.bankms.dto.WithdrawForm;
import com.bankms.exception.InsufficientBalanceException;
import com.bankms.exception.InvalidTransactionException;
import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.Transaction;
import com.bankms.model.Transfer;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.AccountType;
import com.bankms.model.enums.TransactionStatus;
import com.bankms.model.enums.TransactionType;
import com.bankms.repository.AccountRepository;
import com.bankms.repository.TransactionRepository;
import com.bankms.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Customer testCustomer;
    private Account activeAccount1;
    private Account activeAccount2;
    private Account frozenAccount;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("Test User", "test@example.com", "555-0100", "123 Main St");
        testCustomer.setCustomerId(1L);

        activeAccount1 = new Account("101010101011", testCustomer, AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        activeAccount1.setAccountId(1L);

        activeAccount2 = new Account("101010101022", testCustomer, AccountType.CURRENT, new BigDecimal("500.00"), AccountStatus.ACTIVE);
        activeAccount2.setAccountId(2L);

        frozenAccount = new Account("101010101033", testCustomer, AccountType.SAVINGS, new BigDecimal("200.00"), AccountStatus.FROZEN);
        frozenAccount.setAccountId(3L);
    }

    @Test
    @DisplayName("Deposit: Successful deposit updates balance and creates transaction")
    void testSuccessfulDeposit() {
        DepositForm form = new DepositForm("101010101011", new BigDecimal("250.00"), "Deposit test");

        when(accountRepository.findByAccountNumberWithLock("101010101011")).thenReturn(Optional.of(activeAccount1));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.deposit(form);

        assertNotNull(result);
        assertEquals(TransactionType.DEPOSIT, result.getType());
        assertEquals(new BigDecimal("250.00"), result.getAmount());
        assertEquals(new BigDecimal("1250.00"), activeAccount1.getBalance());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        verify(accountRepository).save(activeAccount1);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Withdrawal: Successful withdrawal updates balance and creates transaction")
    void testSuccessfulWithdrawal() {
        WithdrawForm form = new WithdrawForm("101010101011", new BigDecimal("300.00"), "ATM withdraw");

        when(accountRepository.findByAccountNumberWithLock("101010101011")).thenReturn(Optional.of(activeAccount1));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.withdraw(form);

        assertNotNull(result);
        assertEquals(TransactionType.WITHDRAWAL, result.getType());
        assertEquals(new BigDecimal("300.00"), result.getAmount());
        assertEquals(new BigDecimal("700.00"), activeAccount1.getBalance());
        verify(accountRepository).save(activeAccount1);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Withdrawal: Fails with InsufficientBalanceException when balance is too low")
    void testWithdrawInsufficientBalance() {
        WithdrawForm form = new WithdrawForm("101010101011", new BigDecimal("1500.00"), "Overdraft attempt");

        when(accountRepository.findByAccountNumberWithLock("101010101011")).thenReturn(Optional.of(activeAccount1));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(form));
        assertEquals(new BigDecimal("1000.00"), activeAccount1.getBalance(), "Balance must remain unchanged");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Transfer: Successful transfer debits source and credits destination")
    void testSuccessfulTransfer() {
        TransferForm form = new TransferForm("101010101011", "101010101022", new BigDecimal("200.00"), "Transfer test");

        when(accountRepository.findByAccountNumber("101010101011")).thenReturn(Optional.of(activeAccount1));
        when(accountRepository.findByAccountNumber("101010101022")).thenReturn(Optional.of(activeAccount2));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(activeAccount1));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(activeAccount2));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transfer transfer = transactionService.transfer(form);

        assertNotNull(transfer);
        assertEquals(new BigDecimal("200.00"), transfer.getAmount());
        assertEquals(new BigDecimal("800.00"), activeAccount1.getBalance());
        assertEquals(new BigDecimal("700.00"), activeAccount2.getBalance());
        verify(accountRepository).save(activeAccount1);
        verify(accountRepository).save(activeAccount2);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Transfer: Fails on self-transfer attempt")
    void testSelfTransfer() {
        TransferForm form = new TransferForm("101010101011", "101010101011", new BigDecimal("100.00"), "Self transfer");

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(form));
        verify(accountRepository, never()).findByAccountNumber(anyString());
    }

    @Test
    @DisplayName("Transaction: Fails when account is FROZEN")
    void testFrozenAccountTransaction() {
        DepositForm depositForm = new DepositForm("101010101033", new BigDecimal("50.00"), "Deposit");
        when(accountRepository.findByAccountNumberWithLock("101010101033")).thenReturn(Optional.of(frozenAccount));

        assertThrows(InvalidTransactionException.class, () -> transactionService.deposit(depositForm));
    }

    @Test
    @DisplayName("Transaction: Fails when amount is negative or zero")
    void testInvalidAmountZeroOrNegative() {
        DepositForm zeroForm = new DepositForm("101010101011", BigDecimal.ZERO, "Zero");
        assertThrows(InvalidTransactionException.class, () -> transactionService.deposit(zeroForm));

        DepositForm negativeForm = new DepositForm("101010101011", new BigDecimal("-50.00"), "Negative");
        assertThrows(InvalidTransactionException.class, () -> transactionService.deposit(negativeForm));
    }

    @Test
    @DisplayName("Transaction: Fails when amount has more than 2 decimal places")
    void testInvalidAmountDecimals() {
        DepositForm form = new DepositForm("101010101011", new BigDecimal("10.555"), "3 decimals");
        assertThrows(InvalidTransactionException.class, () -> transactionService.deposit(form));
    }
}
