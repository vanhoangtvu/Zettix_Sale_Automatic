package com.zettix.controller;

import com.zettix.entity.Product;
import com.zettix.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public APIs", description = "Public APIs that don't require authentication")
public class PublicController {

    private final ProductRepository productRepository;

    @Operation(summary = "Get all available products", description = "Get list of all active and available products")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productRepository.findAvailableProducts();
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get product by ID", description = "Get product details by product ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Health check", description = "Check if the API is running")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API is running")
    })
    @GetMapping("/health")
    public ResponseEntity<Object> healthCheck() {
        return ResponseEntity.ok(java.util.Map.of(
                "status", "UP",
                "message", "Zettix API is running",
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    @Operation(summary = "Get API info", description = "Get basic information about the API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API info retrieved successfully")
    })
    @GetMapping("/info")
    public ResponseEntity<Object> getApiInfo() {
        return ResponseEntity.ok(java.util.Map.of(
                "name", "Zettix API",
                "version", "1.0.0",
                "description", "Account and License Key Sales Platform",
                "port", 8083,
                "swagger-ui", "http://localhost:8083/api/swagger-ui.html"
        ));
    }
    
    @GetMapping("/test-qr")
    public ResponseEntity<Object> testQR() {
        try {
            // Test QR generation
            String testData = "970436|9889559357|100000|NAP TIEN ZETTIX - TEST123|TEST123";
            
            com.google.zxing.BarcodeFormat format = com.google.zxing.BarcodeFormat.QR_CODE;
            com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(testData, format, 200, 200);
            
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            String base64 = java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "testData", testData,
                "qrSize", base64.length(),
                "message", "QR code generation test successful"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
