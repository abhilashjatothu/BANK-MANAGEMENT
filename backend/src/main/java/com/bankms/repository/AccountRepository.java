package com.bankms.repository;

import com.bankms.model.Account;
import com.bankms.model.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomerCustomerId(Long customerId);

    Page<Account> findByCustomerCustomerId(Long customerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    Optional<Account> findByIdWithLock(@Param("accountId") Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    BigDecimal sumTotalBalance();

    long countByStatus(AccountStatus status);

    @Query("SELECT a FROM Account a WHERE LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.customer.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Account> searchAccounts(@Param("keyword") String keyword, Pageable pageable);
}
