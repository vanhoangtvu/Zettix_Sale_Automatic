package com.zettix.service;

import com.zettix.entity.*;
import com.zettix.repository.*;
import com.zettix.service.VietQRService.VietQRResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final UserPurchaseRepository userPurchaseRepository;
    private final VietQRService vietQRService;

    @Transactional
    public Map<String, Object> createDepositTransaction(User user, BigDecimal amount, String referenceId) {
        try {
            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
            transaction.setAmount(amount);
            transaction.setBalanceBefore(user.getWalletBalance());
            transaction.setBalanceAfter(user.getWalletBalance());
            transaction.setStatus(Transaction.TransactionStatus.PENDING);
            transaction.setReferenceId(referenceId);
            transaction.setDescription("Deposit via VietQR");
            transaction.setExpiresAt(LocalDateTime.now().plusMinutes(30));

            // Generate QR code
            VietQRResponse qrResponse = vietQRService.generateQRCode(amount, referenceId);
            transaction.setQrCodeData(qrResponse.getQrData());

            Transaction savedTransaction = transactionRepository.save(transaction);

            return Map.of(
                    "transaction", savedTransaction,
                    "qrCode", qrResponse.getQrCodeBase64(),
                    "qrData", qrResponse.getQrData(),
                    "accountNumber", qrResponse.getAccountNumber(),
                    "accountName", qrResponse.getAccountName(),
                    "expiresAt", qrResponse.getExpiresAt()
            );

        } catch (Exception e) {
            log.error("Error creating deposit transaction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create deposit transaction", e);
        }
    }

    @Transactional
    public Map<String, Object> createPurchaseTransaction(User user, Long productId) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (!product.getIsActive()) {
                throw new RuntimeException("Product is not available");
            }

            if (product.getAvailableQuantity() <= 0) {
                throw new RuntimeException("Product is out of stock");
            }

            if (user.getWalletBalance().compareTo(product.getPrice()) < 0) {
                throw new RuntimeException("Insufficient wallet balance");
            }

            // Get available account
            List<Account> availableAccounts = accountRepository.findAvailableAccountsByProduct(product);
            if (availableAccounts.isEmpty()) {
                throw new RuntimeException("No accounts available for this product");
            }

            Account account = availableAccounts.get(0);

            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setTransactionType(Transaction.TransactionType.PURCHASE);
            transaction.setAmount(product.getPrice());
            transaction.setBalanceBefore(user.getWalletBalance());
            transaction.setBalanceAfter(user.getWalletBalance().subtract(product.getPrice()));
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setDescription("Purchase: " + product.getName());
            transaction.setProduct(product);
            transaction.setAccount(account);
            transaction.setCompletedAt(LocalDateTime.now());

            Transaction savedTransaction = transactionRepository.save(transaction);

            // Update user wallet
            user.setWalletBalance(user.getWalletBalance().subtract(product.getPrice()));
            userRepository.save(user);

            // Update account status
            account.setStatus(Account.AccountStatus.SOLD);
            account.setSoldToUser(user);
            account.setSoldAt(LocalDateTime.now());
            account.setPurchasePrice(product.getPrice());
            accountRepository.save(account);

            // Update product quantities
            product.setAvailableQuantity(product.getAvailableQuantity() - 1);
            product.setSoldQuantity(product.getSoldQuantity() + 1);
            productRepository.save(product);

            // Create user purchase record
            UserPurchase userPurchase = new UserPurchase();
            userPurchase.setUser(user);
            userPurchase.setProduct(product);
            userPurchase.setAccount(account);
            userPurchase.setTransaction(savedTransaction);
            userPurchase.setPurchasePrice(product.getPrice());
            userPurchase.setAccountUsername(account.getUsername());
            userPurchase.setAccountPassword(account.getPassword());
            userPurchase.setAccountEmail(account.getEmail());
            userPurchase.setAdditionalInfo(account.getAdditionalInfo());
            userPurchase.setStatus(UserPurchase.PurchaseStatus.COMPLETED);

            UserPurchase savedPurchase = userPurchaseRepository.save(userPurchase);

            return Map.of(
                    "transaction", savedTransaction,
                    "purchase", savedPurchase,
                    "account", Map.of(
                            "username", account.getUsername(),
                            "password", account.getPassword(),
                            "email", account.getEmail(),
                            "additionalInfo", account.getAdditionalInfo()
                    )
            );

        } catch (Exception e) {
            log.error("Error creating purchase transaction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create purchase transaction: " + e.getMessage());
        }
    }

    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<UserPurchase> getUserPurchases(User user) {
        return userPurchaseRepository.findUserPurchases(user);
    }

    @Transactional
    public boolean processDepositConfirmation(String referenceId, BigDecimal amount) {
        try {
            Transaction transaction = transactionRepository.findByReferenceId(referenceId)
                    .orElse(null);

            if (transaction == null) {
                log.warn("Transaction not found for reference: {}", referenceId);
                return false;
            }

            if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
                log.warn("Transaction {} is not pending, current status: {}", referenceId, transaction.getStatus());
                return false;
            }

            // Verify amount matches
            if (transaction.getAmount().compareTo(amount) != 0) {
                log.warn("Amount mismatch for transaction {}: expected {}, got {}", 
                        referenceId, transaction.getAmount(), amount);
                return false;
            }

            // Update transaction
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setBalanceAfter(transaction.getUser().getWalletBalance().add(amount));
            transactionRepository.save(transaction);

            // Update user wallet
            User user = transaction.getUser();
            user.setWalletBalance(user.getWalletBalance().add(amount));
            userRepository.save(user);

            log.info("Deposit confirmed for user {}: {} VND", user.getUsername(), amount);
            return true;

        } catch (Exception e) {
            log.error("Error processing deposit confirmation: {}", e.getMessage(), e);
            return false;
        }
    }
}
