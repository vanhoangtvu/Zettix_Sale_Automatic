package com.zettix.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.zettix.entity.EmailTransaction;
import com.zettix.repository.EmailTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_TOKEN = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "gmail-credentials.json";

    @Value("${gmail.application-name}")
    private String applicationName;

    @Value("${vietqr.account-number}")
    private String targetAccountNumber;

    private final EmailTransactionRepository emailTransactionRepository;
    private final TransactionService transactionService;

    public void processNewEmails() {
        try {
            // Check if credentials file exists
            java.io.File credentialsFile = new java.io.File(CREDENTIALS_FILE_PATH);
            if (!credentialsFile.exists()) {
                log.warn("Gmail credentials file not found: {}. Skipping email processing.", CREDENTIALS_FILE_PATH);
                return;
            }

            Gmail service = getGmailService();
            String query = "from:VCBDigibank@info.vietcombank.com.vn subject:Thông báo giao dịch";
            
            ListMessagesResponse response = service.users().messages().list("me")
                    .setQ(query)
                    .setMaxResults(10L)
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("No new emails found");
                return;
            }

            for (Message message : messages) {
                processEmail(service, message);
            }

        } catch (Exception e) {
            log.error("Error processing emails: {}", e.getMessage(), e);
        }
    }

    private void processEmail(Gmail service, Message message) {
        try {
            String messageId = message.getId();
            
            // Check if already processed
            if (emailTransactionRepository.findByEmailId(messageId).isPresent()) {
                return;
            }

            Message fullMessage = service.users().messages().get("me", messageId).execute();
            String subject = getHeaderValue(fullMessage, "Subject");
            String sender = getHeaderValue(fullMessage, "From");
            String body = getMessageBody(fullMessage);

            log.info("Processing email: {} from {}", subject, sender);

            // Parse transaction details
            TransactionDetails details = parseTransactionDetails(body);
            if (details == null) {
                log.warn("Could not parse transaction details from email: {}", messageId);
                return;
            }

            // Check if transaction matches our target account
            if (!targetAccountNumber.equals(details.getAccountNumber())) {
                log.info("Transaction not for our account: {}", details.getAccountNumber());
                return;
            }

            // Save email transaction
            EmailTransaction emailTransaction = new EmailTransaction();
            emailTransaction.setEmailId(messageId);
            emailTransaction.setSenderEmail(sender);
            emailTransaction.setSubject(subject);
            emailTransaction.setBody(body);
            emailTransaction.setBankAccountNumber(details.getAccountNumber());
            emailTransaction.setAmount(details.getAmount());
            emailTransaction.setReferenceCode(details.getReferenceCode());
            emailTransaction.setTransactionDate(details.getTransactionDate());
            emailTransaction.setIsProcessed(false);

            emailTransactionRepository.save(emailTransaction);

            // Process the transaction
            processTransactionConfirmation(emailTransaction);

        } catch (Exception e) {
            log.error("Error processing email {}: {}", message.getId(), e.getMessage(), e);
        }
    }

    private void processTransactionConfirmation(EmailTransaction emailTransaction) {
        try {
            // Find matching pending transaction
            String referenceId = emailTransaction.getReferenceCode();
            if (referenceId == null || referenceId.isEmpty()) {
                log.warn("No reference code found in email transaction");
                return;
            }

            // Clean reference ID to match what we generate
            String cleanRef = referenceId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            
            log.info("Processing transaction confirmation for reference: {} (cleaned: {})", referenceId, cleanRef);

            // Try to match both original and cleaned reference
            boolean processed = transactionService.processDepositConfirmation(cleanRef, emailTransaction.getAmount());
            
            if (!processed && !cleanRef.equals(referenceId)) {
                // Try with original reference if different
                processed = transactionService.processDepositConfirmation(referenceId, emailTransaction.getAmount());
            }

            if (processed) {
                // Mark email as processed
                emailTransaction.setIsProcessed(true);
                emailTransaction.setProcessedAt(java.time.LocalDateTime.now());
                emailTransactionRepository.save(emailTransaction);

                log.info("Successfully processed deposit confirmation for reference: {}", referenceId);
            } else {
                log.warn("No matching pending transaction found for reference: {}", referenceId);
            }

        } catch (Exception e) {
            log.error("Error processing transaction confirmation: {}", e.getMessage(), e);
        }
    }

    private TransactionDetails parseTransactionDetails(String body) {
        try {
            log.debug("Parsing transaction body: {}", body);
            
            // Vietcombank email patterns - support multiple formats
            Pattern[] accountPatterns = {
                Pattern.compile("Tài khoản\\s*:\\s*(\\d+)"),
                Pattern.compile("Số tài khoản\\s*:\\s*(\\d+)"),
                Pattern.compile("TK\\s*:\\s*(\\d+)"),
                Pattern.compile("Account\\s*:\\s*(\\d+)")
            };
            
            Pattern[] amountPatterns = {
                Pattern.compile("Số tiền\\s*:\\s*([\\d,]+)\\s*VND"),
                Pattern.compile("Số tiền\\s*:\\s*([\\d,]+)"),
                Pattern.compile("Amount\\s*:\\s*([\\d,]+)"),
                Pattern.compile("(\\+|-)([\\d,]+)\\s*VND")
            };
            
            Pattern[] referencePatterns = {
                Pattern.compile("Nội dung\\s*:\\s*(.+?)(?=\\n|$)"),
                Pattern.compile("Diễn giải\\s*:\\s*(.+?)(?=\\n|$)"),
                Pattern.compile("Memo\\s*:\\s*(.+?)(?=\\n|$)"),
                Pattern.compile("Content\\s*:\\s*(.+?)(?=\\n|$)"),
                Pattern.compile("NAP TIEN ZETTIX\\s+([A-Z0-9]+)")
            };
            
            Pattern[] datePatterns = {
                Pattern.compile("Thời gian\\s*:\\s*(.+?)(?=\\n|$)"),
                Pattern.compile("Time\\s*:\\s*(.+?)(?=\\n|$)"),
                Pattern.compile("Ngày\\s*:\\s*(.+?)(?=\\n|$)")
            };

            // Find account number
            String accountNumber = null;
            for (Pattern pattern : accountPatterns) {
                Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    accountNumber = matcher.group(1);
                    break;
                }
            }

            // Find amount
            String amountStr = null;
            for (Pattern pattern : amountPatterns) {
                Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    amountStr = matcher.group(matcher.groupCount()).replace(",", "");
                    break;
                }
            }

            // Find reference code
            String referenceCode = "";
            for (Pattern pattern : referencePatterns) {
                Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    String content = matcher.group(1).trim();
                    
                    // Extract ZETTIX reference from content
                    Pattern zettixPattern = Pattern.compile("(?:NAP TIEN ZETTIX|ZETTIX)\\s*([A-Z0-9]+)");
                    Matcher zettixMatcher = zettixPattern.matcher(content);
                    if (zettixMatcher.find()) {
                        referenceCode = zettixMatcher.group(1);
                        break;
                    }
                    
                    // If no ZETTIX pattern, use the whole content but clean it
                    referenceCode = content.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                    break;
                }
            }

            // Find date
            String dateStr = "";
            for (Pattern pattern : datePatterns) {
                Matcher matcher = pattern.matcher(body);
                if (matcher.find()) {
                    dateStr = matcher.group(1).trim();
                    break;
                }
            }

            if (accountNumber == null || amountStr == null) {
                log.warn("Could not extract required fields - account: {}, amount: {}", accountNumber, amountStr);
                return null;
            }

            TransactionDetails details = new TransactionDetails();
            details.setAccountNumber(accountNumber);
            details.setAmount(new java.math.BigDecimal(amountStr));
            details.setReferenceCode(referenceCode);
            details.setTransactionDate(parseTransactionDate(dateStr));

            log.info("Parsed transaction details - Account: {}, Amount: {}, Reference: {}", 
                    accountNumber, amountStr, referenceCode);

            return details;

        } catch (Exception e) {
            log.error("Error parsing transaction details: {}", e.getMessage(), e);
            return null;
        }
    }

    private java.time.LocalDateTime parseTransactionDate(String dateStr) {
        try {
            // Parse Vietcombank date format
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return java.time.LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("Could not parse transaction date: {}", dateStr);
            return java.time.LocalDateTime.now();
        }
    }

    private String getHeaderValue(Message message, String headerName) {
        return message.getPayload().getHeaders().stream()
                .filter(header -> headerName.equals(header.getName()))
                .map(header -> header.getValue())
                .findFirst()
                .orElse("");
    }

    private String getMessageBody(Message message) {
        try {
            return new String(java.util.Base64.getUrlDecoder().decode(
                    message.getPayload().getBody().getData()));
        } catch (Exception e) {
            return "";
        }
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Try to find credentials file in current directory first
        java.io.File credentialsFile = new java.io.File(CREDENTIALS_FILE_PATH);
        if (!credentialsFile.exists()) {
            // Try to find in resources
            InputStream in = GmailService.class.getResourceAsStream("/" + CREDENTIALS_FILE_PATH);
            if (in == null) {
                throw new FileNotFoundException("Credentials file not found: " + CREDENTIALS_FILE_PATH + 
                    ". Please create this file in the backend directory.");
            }
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            in.close();
            
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_TOKEN)))
                    .setAccessType("offline")
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
        
        // Load from file
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, 
            new java.io.FileReader(credentialsFile));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_TOKEN)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private Gmail getGmailService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(applicationName)
                .build();
    }

    @lombok.Data
    private static class TransactionDetails {
        private String accountNumber;
        private java.math.BigDecimal amount;
        private String referenceCode;
        private java.time.LocalDateTime transactionDate;
    }
}
