package com.zettix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.AVAILABLE;
    
    @Column(name = "purchase_price", precision = 19, scale = 2)
    private java.math.BigDecimal purchasePrice;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "sold_at")
    private LocalDateTime soldAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_to_user_id")
    private User soldToUser;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum AccountStatus {
        AVAILABLE, SOLD, RESERVED, EXPIRED
    }
}
