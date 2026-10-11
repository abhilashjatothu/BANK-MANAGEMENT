package com.bankms.repository;

import com.bankms.model.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByReferenceNo(String referenceNo);

    @Query("SELECT t FROM Transfer t WHERE t.fromAccount.customer.customerId = :customerId " +
           "OR t.toAccount.customer.customerId = :customerId ORDER BY t.dateTime DESC")
    Page<Transfer> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
}
