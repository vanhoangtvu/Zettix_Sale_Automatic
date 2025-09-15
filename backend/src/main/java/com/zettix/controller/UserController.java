package com.zettix.controller;

import com.zettix.dto.request.DepositRequest;
import com.zettix.dto.request.PurchaseRequest;
import com.zettix.entity.User;
import com.zettix.service.TransactionService;
import com.zettix.service.UserService;
import com.zettix.service.VietQRService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Operations", description = "APIs for user operations like profile, deposit, purchase")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final VietQRService vietQRService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unable to get profile information"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody User user, Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            currentUser.setFullName(user.getFullName());
            currentUser.setPhoneNumber(user.getPhoneNumber());
            currentUser.setEmail(user.getEmail());
            
            User updatedUser = userService.save(currentUser);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to update profile: " + e.getMessage()));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(@Valid @RequestBody DepositRequest request, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            String referenceId = vietQRService.generateReferenceId();
            
            var result = transactionService.createDepositTransaction(user, request.getAmount(), referenceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create deposit: " + e.getMessage()));
        }
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseProduct(@Valid @RequestBody PurchaseRequest request, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            var result = transactionService.createPurchaseTransaction(user, request.getProductId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to purchase product: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            var transactions = transactionService.getUserTransactions(user);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/purchases")
    public ResponseEntity<?> getPurchases(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            var purchases = transactionService.getUserPurchases(user);
            return ResponseEntity.ok(purchases);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get purchases: " + e.getMessage()));
        }
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<?> getWalletBalance(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            return ResponseEntity.ok(Map.of("balance", user.getWalletBalance()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get wallet balance"));
        }
    }
}
