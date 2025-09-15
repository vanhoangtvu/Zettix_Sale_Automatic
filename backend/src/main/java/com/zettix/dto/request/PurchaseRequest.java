package com.zettix.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
}
