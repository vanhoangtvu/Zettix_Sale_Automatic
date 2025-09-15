package com.zettix.repository;

import com.zettix.entity.Account;
import com.zettix.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    List<Account> findByProduct(Product product);
    
    List<Account> findByStatus(Account.AccountStatus status);
    
    List<Account> findByProductAndStatus(Product product, Account.AccountStatus status);
    
    @Query("SELECT a FROM Account a WHERE a.product = :product AND a.status = 'AVAILABLE' ORDER BY a.createdAt ASC")
    List<Account> findAvailableAccountsByProduct(@Param("product") Product product);
    
    @Query("SELECT a FROM Account a WHERE a.status = 'AVAILABLE' ORDER BY a.createdAt ASC")
    List<Account> findAvailableAccounts();
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.product = :product AND a.status = 'AVAILABLE'")
    Long countAvailableAccountsByProduct(@Param("product") Product product);
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = 'SOLD'")
    Long countSoldAccounts();
    
    @Query("SELECT a FROM Account a WHERE a.soldToUser.id = :userId ORDER BY a.soldAt DESC")
    List<Account> findAccountsByUserId(@Param("userId") Long userId);
}
