package com.zettix.repository;

import com.zettix.entity.EmailTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTransactionRepository extends JpaRepository<EmailTransaction, Long> {
    
    Optional<EmailTransaction> findByEmailId(String emailId);
    
    List<EmailTransaction> findByIsProcessedFalse();
    
    List<EmailTransaction> findByIsProcessedTrue();
    
    @Query("SELECT et FROM EmailTransaction et WHERE et.isProcessed = false ORDER BY et.createdAt ASC")
    List<EmailTransaction> findUnprocessedEmailTransactions();
    
    @Query("SELECT et FROM EmailTransaction et WHERE et.bankAccountNumber = :accountNumber AND et.amount = :amount AND et.referenceCode = :referenceCode")
    List<EmailTransaction> findMatchingTransactions(@Param("accountNumber") String accountNumber, 
                                                   @Param("amount") java.math.BigDecimal amount, 
                                                   @Param("referenceCode") String referenceCode);
    
    @Query("SELECT COUNT(et) FROM EmailTransaction et WHERE et.isProcessed = true AND et.createdAt >= :fromDate")
    Long countProcessedTransactionsSince(@Param("fromDate") LocalDateTime fromDate);
}
