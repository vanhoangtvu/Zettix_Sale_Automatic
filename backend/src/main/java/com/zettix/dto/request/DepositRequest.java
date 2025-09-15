package com.zettix.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum deposit amount is 10,000 VND")
    private BigDecimal amount;
}
