package com.zettix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "product_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductType productType;
    
    @Column(name = "price", precision = 19, scale = 2, nullable = false)
    private BigDecimal price;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "total_quantity")
    private Integer totalQuantity = 0;
    
    @Column(name = "available_quantity")
    private Integer availableQuantity = 0;
    
    @Column(name = "sold_quantity")
    private Integer soldQuantity = 0;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum ProductType {
        ACCOUNT, LICENSE_KEY, GAME_ACCOUNT, SOFTWARE_LICENSE
    }
}
