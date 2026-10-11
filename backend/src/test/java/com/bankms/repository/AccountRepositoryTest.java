package com.bankms.repository;

import com.bankms.model.Account;
import com.bankms.model.Customer;
import com.bankms.model.enums.AccountStatus;
import com.bankms.model.enums.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Repository: Successfully save and find account by account number")
    void testSaveAndFindByAccountNumber() {
        Customer customer = new Customer("Jane Doe", "jane@example.com", "555-1234", "100 Avenue");
        entityManager.persist(customer);

        Account account = new Account("101099887766", customer, AccountType.SAVINGS, new BigDecimal("1200.00"), AccountStatus.ACTIVE);
        accountRepository.save(account);

        Optional<Account> found = accountRepository.findByAccountNumber("101099887766");
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("1200.00"), found.get().getBalance());
        assertEquals("Jane Doe", found.get().getCustomer().getName());
    }

    @Test
    @DisplayName("Repository: sumTotalBalance correctly calculates total balance")
    void testSumTotalBalance() {
        Customer customer = new Customer("Bob Smith", "bob@example.com", "555-4321", "200 Road");
        entityManager.persist(customer);

        Account acc1 = new Account("101011112222", customer, AccountType.SAVINGS, new BigDecimal("1500.50"), AccountStatus.ACTIVE);
        Account acc2 = new Account("101033334444", customer, AccountType.CURRENT, new BigDecimal("3500.25"), AccountStatus.ACTIVE);
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        BigDecimal total = accountRepository.sumTotalBalance();
        assertEquals(new BigDecimal("5000.75"), total);
    }
}
