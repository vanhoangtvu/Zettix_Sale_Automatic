package com.zettix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_purchases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPurchase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @Column(name = "purchase_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal purchasePrice;
    
    @Column(name = "account_username")
    private String accountUsername;
    
    @Column(name = "account_password")
    private String accountPassword;
    
    @Column(name = "account_email")
    private String accountEmail;
    
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status = PurchaseStatus.COMPLETED;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum PurchaseStatus {
        PENDING, COMPLETED, CANCELLED, REFUNDED
    }
}
