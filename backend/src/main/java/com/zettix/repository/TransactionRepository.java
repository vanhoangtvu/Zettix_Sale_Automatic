package com.zettix.repository;

import com.zettix.entity.Transaction;
import com.zettix.entity.User;
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
    
    List<Transaction> findByUser(User user);
    
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    
    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);
    
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    List<Transaction> findByUserAndTransactionType(User user, Transaction.TransactionType transactionType);
    
    Optional<Transaction> findByReferenceId(String referenceId);
    
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.transactionType = :type ORDER BY t.createdAt DESC")
    List<Transaction> findUserTransactionsByType(@Param("user") User user, @Param("type") Transaction.TransactionType type);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.expiresAt < :now")
    List<Transaction> findExpiredTransactions(@Param("now") LocalDateTime now);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = 'DEPOSIT' AND t.status = 'COMPLETED'")
    BigDecimal getTotalDeposits();
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionType = 'PURCHASE' AND t.status = 'COMPLETED'")
    BigDecimal getTotalPurchases();
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionType = 'DEPOSIT' AND t.status = 'COMPLETED'")
    Long countCompletedDeposits();
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionType = 'PURCHASE' AND t.status = 'COMPLETED'")
    Long countCompletedPurchases();
}
