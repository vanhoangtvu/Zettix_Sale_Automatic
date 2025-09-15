package com.zettix.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

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
public class VietQRServiceTest {

    @Autowired
    private VietQRService vietQRService;

    @Test
    public void testGenerateQRCode() {
        try {
            String referenceId = vietQRService.generateReferenceId();
            BigDecimal amount = new BigDecimal("100000");
            
            VietQRService.VietQRResponse response = vietQRService.generateQRCode(amount, referenceId);
            
            System.out.println("=== VietQR Full Information Test Result ===");
            System.out.println("Reference ID: " + referenceId);
            System.out.println("Amount: " + amount + " VND");
            System.out.println("QR Data: " + response.getQrData());
            System.out.println("Account Number: " + response.getAccountNumber());
            System.out.println("Account Name: " + response.getAccountName());
            System.out.println("Bank Code: " + response.getBankCode());
            System.out.println("Expires At: " + response.getExpiresAt());
            System.out.println("QR Code Base64 Length: " + response.getQrCodeBase64().length());
            
            // Analyze QR data structure
            String qrData = response.getQrData();
            System.out.println("\n=== QR Data Structure Analysis ===");
            
            // Parse EMVCo fields
            analyzeEMVCoField(qrData, "01", "Point of Initiation Method");
            analyzeEMVCoField(qrData, "38", "Merchant Account Information");
            analyzeEMVCoField(qrData, "52", "Merchant Category Code");
            analyzeEMVCoField(qrData, "53", "Transaction Currency");
            analyzeEMVCoField(qrData, "54", "Transaction Amount");
            analyzeEMVCoField(qrData, "58", "Country Code");
            analyzeEMVCoField(qrData, "59", "Merchant Name");
            analyzeEMVCoField(qrData, "60", "Merchant City");
            analyzeEMVCoField(qrData, "62", "Additional Data Field");
            analyzeEMVCoField(qrData, "63", "CRC");
            
            // Detailed analysis of field 62 (Additional Data)
            int field62Index = qrData.indexOf("62");
            if (field62Index >= 0) {
                System.out.println("\n=== Field 62 (Additional Data) Details ===");
                String field62 = extractEMVCoFieldValue(qrData, "62");
                if (field62 != null) {
                    System.out.println("Field 62 content: " + field62);
                    analyzeEMVCoSubField(field62, "01", "Store Label");
                    analyzeEMVCoSubField(field62, "05", "Bill Number (Transaction Content)");
                    analyzeEMVCoSubField(field62, "07", "Reference Label");
                    analyzeEMVCoSubField(field62, "08", "Purpose of Transaction");
                }
            }
            
            System.out.println("\n=== Expected Banking App Behavior ===");
            String cleanRef = referenceId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            System.out.println("✓ Amount: " + amount + " VND (LOCKED - cannot edit)");
            System.out.println("✓ Recipient: " + response.getAccountName() + " (LOCKED - cannot edit)");
            System.out.println("✓ Account: " + response.getAccountNumber() + " (LOCKED - cannot edit)");
            System.out.println("✓ Bank: Vietcombank (" + response.getBankCode() + ") (LOCKED - cannot edit)");
            System.out.println("✓ Content: NAP TIEN ZETTIX " + cleanRef + " (LOCKED - cannot edit)");
            System.out.println("✓ User action: ONLY needs to confirm/authorize payment");
            
            System.out.println("\n=== Email Processing Expected ===");
            System.out.println("• Email will contain: 'NAP TIEN ZETTIX " + cleanRef + "'");
            System.out.println("• System will extract: '" + cleanRef + "'");
            System.out.println("• Match with transaction reference: '" + cleanRef + "'");
            System.out.println("• Auto-confirm and add balance to user wallet");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void analyzeEMVCoField(String qrData, String fieldId, String fieldName) {
        String value = extractEMVCoFieldValue(qrData, fieldId);
        if (value != null) {
            System.out.println("Field " + fieldId + " (" + fieldName + "): " + value);
        }
    }
    
    private void analyzeEMVCoSubField(String parentField, String subfieldId, String subfieldName) {
        String value = extractEMVCoFieldValue(parentField, subfieldId);
        if (value != null) {
            System.out.println("  Subfield " + subfieldId + " (" + subfieldName + "): " + value);
        }
    }
    
    private String extractEMVCoFieldValue(String data, String fieldId) {
        int index = data.indexOf(fieldId);
        if (index >= 0 && index + 4 < data.length()) {
            try {
                String lengthStr = data.substring(index + 2, index + 4);
                int length = Integer.parseInt(lengthStr);
                if (index + 4 + length <= data.length()) {
                    return data.substring(index + 4, index + 4 + length);
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return null;
    }
}
