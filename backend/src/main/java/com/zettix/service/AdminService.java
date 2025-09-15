package com.zettix.service;

import com.zettix.dto.request.CreateProductRequest;
import com.zettix.dto.request.UpdateProductRequest;
import com.zettix.entity.*;
import com.zettix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserPurchaseRepository userPurchaseRepository;

    // User Management
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User toggleUserStatus(Long id) {
        User user = getUserById(id);
        user.setIsActive(!user.getIsActive());
        return userRepository.save(user);
    }

    @Transactional
    public User toggleUserLock(Long id) {
        User user = getUserById(id);
        user.setIsLocked(!user.getIsLocked());
        return userRepository.save(user);
    }

    // Product Management
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setProductType(request.getProductType());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());
        product.setTotalQuantity(request.getTotalQuantity());
        product.setAvailableQuantity(request.getTotalQuantity());
        product.setSoldQuantity(0);
        product.setIsActive(true);

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getTotalQuantity() != null) {
            int quantityDiff = request.getTotalQuantity() - product.getTotalQuantity();
            product.setTotalQuantity(request.getTotalQuantity());
            product.setAvailableQuantity(product.getAvailableQuantity() + quantityDiff);
        }
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if product has any sold accounts
        if (product.getSoldQuantity() > 0) {
            throw new RuntimeException("Cannot delete product with sold accounts");
        }

        productRepository.delete(product);
    }

    @Transactional
    public Product toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setIsActive(!product.getIsActive());
        return productRepository.save(product);
    }

    // Account Management
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public Map<String, Object> bulkImportAccounts(Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> accountsData = (List<Map<String, Object>>) request.get("accounts");

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            int importedCount = 0;
            for (Map<String, Object> accountData : accountsData) {
                Account account = new Account();
                account.setProduct(product);
                account.setUsername(accountData.get("username").toString());
                account.setPassword(accountData.get("password").toString());
                account.setEmail(accountData.get("email") != null ? accountData.get("email").toString() : null);
                account.setAdditionalInfo(accountData.get("additionalInfo") != null ? accountData.get("additionalInfo").toString() : null);
                account.setStatus(Account.AccountStatus.AVAILABLE);

                accountRepository.save(account);
                importedCount++;
            }

            // Update product quantities
            product.setTotalQuantity(product.getTotalQuantity() + importedCount);
            product.setAvailableQuantity(product.getAvailableQuantity() + importedCount);
            productRepository.save(product);

            return Map.of(
                    "message", "Accounts imported successfully",
                    "importedCount", importedCount,
                    "productId", productId
            );

        } catch (Exception e) {
            log.error("Error importing accounts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to import accounts: " + e.getMessage());
        }
    }

    // Dashboard Statistics
    public Map<String, Object> getDashboardStats() {
        try {
            Long totalUsers = userRepository.countActiveUsers();
            Long totalProducts = productRepository.countActiveProducts();
            Long totalSoldAccounts = accountRepository.countSoldAccounts();
            Long totalSoldQuantity = productRepository.getTotalSoldQuantity();
            BigDecimal totalRevenue = userPurchaseRepository.getTotalRevenue();
            BigDecimal totalWalletBalance = userRepository.getTotalWalletBalance();
            Long completedDeposits = transactionRepository.countCompletedDeposits();
            Long completedPurchases = transactionRepository.countCompletedPurchases();

            return Map.of(
                    "totalUsers", totalUsers,
                    "totalProducts", totalProducts,
                    "totalSoldAccounts", totalSoldAccounts,
                    "totalSoldQuantity", totalSoldQuantity,
                    "totalRevenue", totalRevenue,
                    "totalWalletBalance", totalWalletBalance,
                    "completedDeposits", completedDeposits,
                    "completedPurchases", completedPurchases
            );

        } catch (Exception e) {
            log.error("Error getting dashboard stats: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get dashboard statistics", e);
        }
    }

    // Transaction Management
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
