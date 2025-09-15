package com.zettix.repository;

import com.zettix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    List<User> findByRole(User.Role role);
    
    List<User> findByIsActiveTrue();
    
    List<User> findByIsLockedFalse();
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findActiveUsersByRole(@Param("role") User.Role role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER' AND u.isActive = true")
    Long countActiveUsers();
    
    @Query("SELECT SUM(u.walletBalance) FROM User u WHERE u.isActive = true")
    java.math.BigDecimal getTotalWalletBalance();
}
