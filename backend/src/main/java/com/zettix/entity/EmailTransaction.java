package com.zettix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email_id", unique = true, nullable = false)
    private String emailId;
    
    @Column(name = "sender_email")
    private String senderEmail;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;
    
    @Column(name = "bank_account_number")
    private String bankAccountNumber;
    
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "reference_code")
    private String referenceCode;
    
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
    
    @Column(name = "is_processed")
    private Boolean isProcessed = false;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
