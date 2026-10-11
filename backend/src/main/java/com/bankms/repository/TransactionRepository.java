package com.bankms.repository;

import com.bankms.model.Transaction;
import com.bankms.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceNo(String referenceNo);

    Page<Transaction> findByAccountAccountId(Long accountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.account.customer.customerId = :customerId ORDER BY t.dateTime DESC")
    Page<Transaction> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:accountId IS NULL OR t.account.accountId = :accountId) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:startDate IS NULL OR t.dateTime >= :startDate) AND " +
           "(:endDate IS NULL OR t.dateTime <= :endDate) " +
           "ORDER BY t.dateTime DESC")
    Page<Transaction> findFilteredTransactions(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.account.customer.customerId = :customerId AND " +
           "(:accountId IS NULL OR t.account.accountId = :accountId) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:startDate IS NULL OR t.dateTime >= :startDate) AND " +
           "(:endDate IS NULL OR t.dateTime <= :endDate) " +
           "ORDER BY t.dateTime DESC")
    Page<Transaction> findFilteredCustomerTransactions(
            @Param("customerId") Long customerId,
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.dateTime >= :startOfDay AND t.dateTime <= :endOfDay")
    long countTransactionsByDateRange(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.dateTime >= :startOfDay AND t.dateTime <= :endOfDay AND t.status = 'SUCCESS'")
    BigDecimal sumVolumeByDateRange(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT t.type as type, COUNT(t) as count, COALESCE(SUM(t.amount), 0) as totalAmount " +
           "FROM Transaction t WHERE t.status = 'SUCCESS' GROUP BY t.type")
    List<Object[]> findTransactionTotalsPerType();

    List<Transaction> findTop5ByOrderByDateTimeDesc();

    @Query("SELECT t FROM Transaction t WHERE t.account.customer.customerId = :customerId ORDER BY t.dateTime DESC")
    List<Transaction> findTop5ByCustomer(@Param("customerId") Long customerId, Pageable pageable);
}
