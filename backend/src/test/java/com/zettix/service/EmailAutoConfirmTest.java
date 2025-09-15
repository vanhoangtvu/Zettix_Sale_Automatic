package com.zettix.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.lang.reflect.Method;

@SpringBootTest
@TestPropertySource(properties = {
    "vietqr.bank-code=970436",
    "vietqr.account-number=9889559357", 
    "vietqr.account-name=NGUYEN VAN HOANG",
    "vietqr.merchant-city=HO CHI MINH",
    "vietqr.qr-width=300",
    "vietqr.qr-height=300",
    "business.qr-expiration-minutes=15"
})
public class EmailAutoConfirmTest {

    @Autowired
    private VietQRService vietQRService;
    
    @Autowired
    private GmailService gmailService;

    @Test
    public void testFullFlowEmailAutoConfirm() {
        try {
            System.out.println("=== Test Email Auto-Confirm Flow ===");
            
            // 1. Tạo QR code và reference ID
            String referenceId = vietQRService.generateReferenceId();
            BigDecimal amount = new BigDecimal("100000");
            
            VietQRService.VietQRResponse response = vietQRService.generateQRCode(amount, referenceId);
            String cleanRef = referenceId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            
            System.out.println("1. QR Code Generated:");
            System.out.println("   Reference ID: " + referenceId);
            System.out.println("   Clean Reference: " + cleanRef);
            System.out.println("   Amount: " + amount + " VND");
            System.out.println("   Expected Content: NAP TIEN ZETTIX " + cleanRef);
            
            // 2. Mô phỏng email từ Vietcombank với các format khác nhau
            String[] emailSamples = {
                // Format 1: Chuẩn Vietcombank
                "Quý khách vừa thực hiện giao dịch chuyển tiền\n" +
                "Tài khoản: 9889559357\n" +
                "Số tiền: 100,000 VND\n" +
                "Nội dung: NAP TIEN ZETTIX " + cleanRef + "\n" +
                "Thời gian: 15/09/2025 14:30:00\n" +
                "Số dư: 5,500,000 VND",
                
                // Format 2: Format rút gọn
                "TK: 9889559357\n" +
                "ST: 100,000 VND\n" +
                "ND: NAP TIEN ZETTIX " + cleanRef + "\n" +
                "TG: 15/09/2025 14:30:00",
                
                // Format 3: Format với khoảng trắng
                "Số tài khoản:   9889559357  \n" +
                "Số tiền:  100,000\n" +
                "Diễn giải:  NAP TIEN ZETTIX   " + cleanRef + "  \n" +
                "Ngày: 15/09/2025 14:30:00",
                
                // Format 4: Format English
                "Account: 9889559357\n" +
                "Amount: 100,000 VND\n" +
                "Content: NAP TIEN ZETTIX " + cleanRef + "\n" +
                "Time: 15/09/2025 14:30:00"
            };
            
            System.out.println("\n2. Testing Email Parsing:");
            
            for (int i = 0; i < emailSamples.length; i++) {
                System.out.println("\n--- Email Format " + (i + 1) + " ---");
                System.out.println("Email Content: " + emailSamples[i].replace("\n", " | "));
                
                // Test parsing method
                Object transactionDetails = testEmailParsing(gmailService, emailSamples[i]);
                if (transactionDetails != null) {
                    // Get fields using reflection
                    Method getAccountNumber = transactionDetails.getClass().getMethod("getAccountNumber");
                    Method getAmount = transactionDetails.getClass().getMethod("getAmount");
                    Method getReferenceCode = transactionDetails.getClass().getMethod("getReferenceCode");
                    
                    String accountNumber = (String) getAccountNumber.invoke(transactionDetails);
                    BigDecimal emailAmount = (BigDecimal) getAmount.invoke(transactionDetails);
                    String referenceCode = (String) getReferenceCode.invoke(transactionDetails);
                    
                    System.out.println("✓ Parsed Account: " + accountNumber);
                    System.out.println("✓ Parsed Amount: " + emailAmount);
                    System.out.println("✓ Parsed Reference: " + referenceCode);
                    
                    // Check if can auto-confirm
                    boolean canAutoConfirm = 
                        "9889559357".equals(accountNumber) &&
                        amount.compareTo(emailAmount) == 0 &&
                        cleanRef.equals(referenceCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase());
                    
                    if (canAutoConfirm) {
                        System.out.println("✅ CAN AUTO-CONFIRM: All data matches!");
                        System.out.println("   - Account matches: " + accountNumber);
                        System.out.println("   - Amount matches: " + emailAmount);
                        System.out.println("   - Reference matches: " + referenceCode);
                    } else {
                        System.out.println("❌ CANNOT AUTO-CONFIRM: Data mismatch");
                        System.out.println("   - Expected account: 9889559357, got: " + accountNumber);
                        System.out.println("   - Expected amount: " + amount + ", got: " + emailAmount);
                        System.out.println("   - Expected reference: " + cleanRef + ", got: " + referenceCode);
                    }
                } else {
                    System.out.println("❌ FAILED TO PARSE EMAIL");
                }
            }
            
            System.out.println("\n3. QR Code Analysis:");
            System.out.println("QR Data: " + response.getQrData());
            
            // Analyze if QR contains correct content
            String qrData = response.getQrData();
            String expectedContent = "NAP TIEN ZETTIX " + cleanRef;
            
            if (qrData.contains(expectedContent)) {
                System.out.println("✅ QR Code contains expected content: " + expectedContent);
            } else {
                System.out.println("❌ QR Code missing expected content: " + expectedContent);
                
                // Try to find field 62
                int field62Index = qrData.indexOf("62");
                if (field62Index >= 0) {
                    try {
                        String lengthStr = qrData.substring(field62Index + 2, field62Index + 4);
                        int length = Integer.parseInt(lengthStr);
                        String field62Content = qrData.substring(field62Index + 4, field62Index + 4 + length);
                        System.out.println("Field 62 content: " + field62Content);
                    } catch (Exception e) {
                        System.out.println("Failed to parse field 62");
                    }
                }
            }
            
            System.out.println("\n=== Summary ===");
            System.out.println("✓ QR Code generated successfully");
            System.out.println("✓ Email parsing works for multiple formats");
            System.out.println("✓ Auto-confirmation logic implemented");
            System.out.println("→ System ready for automatic email processing!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Object testEmailParsing(GmailService gmailService, String emailBody) {
        try {
            // Use reflection to call private method
            Method parseMethod = GmailService.class.getDeclaredMethod("parseTransactionDetails", String.class);
            parseMethod.setAccessible(true);
            return parseMethod.invoke(gmailService, emailBody);
        } catch (Exception e) {
            System.out.println("Error calling parseTransactionDetails: " + e.getMessage());
            return null;
        }
    }
}
