package com.zettix.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    
    private String name;
    
    private String description;
    
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    private String imageUrl;
    
    private String category;
    
    private Integer totalQuantity;
    
    private Boolean isActive;
}
