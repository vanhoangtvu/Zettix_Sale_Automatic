package com.zettix.controller;

import com.zettix.dto.request.CreateProductRequest;
import com.zettix.dto.request.UpdateProductRequest;
import com.zettix.entity.Product;
import com.zettix.entity.User;
import com.zettix.service.AdminService;
import com.zettix.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "APIs for admin operations like user management, product management")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    // User Management
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            var users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            var user = adminService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get user: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            var user = adminService.toggleUserStatus(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to toggle user status: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/toggle-lock")
    public ResponseEntity<?> toggleUserLock(@PathVariable Long id) {
        try {
            var user = adminService.toggleUserLock(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to toggle user lock: " + e.getMessage()));
        }
    }

    // Product Management
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        try {
            var products = adminService.getAllProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get products: " + e.getMessage()));
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@Valid @RequestBody CreateProductRequest request) {
        try {
            var product = adminService.createProduct(request);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create product: " + e.getMessage()));
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        try {
            var product = adminService.updateProduct(id, request);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to update product: " + e.getMessage()));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            adminService.deleteProduct(id);
            return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to delete product: " + e.getMessage()));
        }
    }

    @PutMapping("/products/{id}/toggle-status")
    public ResponseEntity<?> toggleProductStatus(@PathVariable Long id) {
        try {
            var product = adminService.toggleProductStatus(id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to toggle product status: " + e.getMessage()));
        }
    }

    // Account Management
    @GetMapping("/accounts")
    public ResponseEntity<?> getAllAccounts() {
        try {
            var accounts = adminService.getAllAccounts();
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get accounts: " + e.getMessage()));
        }
    }

    @PostMapping("/accounts/bulk-import")
    public ResponseEntity<?> bulkImportAccounts(@RequestBody Map<String, Object> request) {
        try {
            var result = adminService.bulkImportAccounts(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to import accounts: " + e.getMessage()));
        }
    }

    // Dashboard Statistics
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            var stats = adminService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get dashboard stats: " + e.getMessage()));
        }
    }

    // Transaction Management
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        try {
            var transactions = adminService.getAllTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        try {
            var transaction = adminService.getTransactionById(id);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get transaction: " + e.getMessage()));
        }
    }
}
