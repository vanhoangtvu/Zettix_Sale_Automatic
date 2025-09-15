package com.zettix.repository;

import com.zettix.entity.User;
import com.zettix.entity.UserPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface UserPurchaseRepository extends JpaRepository<UserPurchase, Long> {
    
    List<UserPurchase> findByUser(User user);
    
    List<UserPurchase> findByUserOrderByCreatedAtDesc(User user);
    
    List<UserPurchase> findByStatus(UserPurchase.PurchaseStatus status);
    
    @Query("SELECT up FROM UserPurchase up WHERE up.user = :user ORDER BY up.createdAt DESC")
    List<UserPurchase> findUserPurchases(@Param("user") User user);
    
    @Query("SELECT up FROM UserPurchase up WHERE up.user = :user AND up.status = 'COMPLETED' ORDER BY up.createdAt DESC")
    List<UserPurchase> findCompletedUserPurchases(@Param("user") User user);
    
    @Query("SELECT SUM(up.purchasePrice) FROM UserPurchase up WHERE up.user = :user AND up.status = 'COMPLETED'")
    BigDecimal getTotalSpentByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(up) FROM UserPurchase up WHERE up.status = 'COMPLETED'")
    Long countCompletedPurchases();
    
    @Query("SELECT SUM(up.purchasePrice) FROM UserPurchase up WHERE up.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();
}
