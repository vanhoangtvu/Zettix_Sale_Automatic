package com.zettix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "balance_before", precision = 19, scale = 2)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(name = "reference_id", unique = true)
    private String referenceId;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "bank_transaction_id")
    private String bankTransactionId;
    
    @Column(name = "qr_code_data")
    private String qrCodeData;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum TransactionType {
        DEPOSIT, PURCHASE, REFUND, ADMIN_ADJUSTMENT
    }
    
    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, EXPIRED, CANCELLED
    }
}
