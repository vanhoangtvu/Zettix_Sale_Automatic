package com.zettix.service;

import java.math.BigDecimal;

/**
 * Test VietQR format mới theo chuẩn thực tế.
 */
public class VietQRTestNewFormat {
    
    public static void main(String[] args) {
        // Giả lập VietQRService
        VietQRTestService service = new VietQRTestService(
            "970422",  // VCB bank code
            "1021282569",  // account number
            "TRAN VAN A",  // account name
            "HA NOI"  // city
        );
        
        // Test với amount và referenceId
        BigDecimal amount = new BigDecimal("50000");
        String referenceId = "ZETTIX123456";
        
        String qrData = service.buildVietQRPayload(amount, referenceId);
        System.out.println("QR Data: " + qrData);
        System.out.println("Length: " + qrData.length());
        
        // Phân tích từng field
        analyzeQRData(qrData);
    }
    
    private static void analyzeQRData(String qrData) {
        System.out.println("\n=== PHÂN TÍCH QR DATA ===");
        
        int pos = 0;
        while (pos < qrData.length() - 4) {
            String fieldId = qrData.substring(pos, pos + 2);
            String lengthStr = qrData.substring(pos + 2, pos + 4);
            int length = Integer.parseInt(lengthStr);
            String value = qrData.substring(pos + 4, pos + 4 + length);
            
            System.out.println("Field " + fieldId + ": " + value + " (length: " + length + ")");
            
            // Nếu là field 38 hoặc 62, phân tích subfield
            if ("38".equals(fieldId) || "62".equals(fieldId)) {
                analyzeSubfields(value, fieldId);
            }
            
            pos += 4 + length;
        }
    }
    
    private static void analyzeSubfields(String data, String parentField) {
        System.out.println("  Subfields of " + parentField + ":");
        
        int pos = 0;
        while (pos < data.length() - 2) {
            String subfieldId = data.substring(pos, pos + 2);
            String lengthStr = data.substring(pos + 2, pos + 4);
            int length = Integer.parseInt(lengthStr);
            String value = data.substring(pos + 4, pos + 4 + length);
            
            System.out.println("    " + subfieldId + ": " + value);
            
            pos += 4 + length;
        }
    }
    
    // Simplified VietQRService for testing
    private static class VietQRTestService {
        private final String bankCode;
        private final String accountNumber;
        private final String accountName;
        private final String merchantCity;
        
        public VietQRTestService(String bankCode, String accountNumber, String accountName, String merchantCity) {
            this.bankCode = bankCode;
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.merchantCity = merchantCity;
        }
        
        public String buildVietQRPayload(BigDecimal amount, String referenceId) {
            String cleanRef = referenceId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            String transactionContent = "NAP TIEN ZETTIX " + cleanRef;

            StringBuilder sb = new StringBuilder();

            // 00: Payload Format Indicator
            sb.append("00").append("02").append("01");

            // 01: Point of Initiation Method (12 = static)
            sb.append("01").append("02").append("12");

            // 38: Merchant Account Information
            StringBuilder mai = new StringBuilder();
            mai.append("00").append("10").append("A000000727");
            String acquirerData = "0006" + bankCode + "0110" + accountNumber;
            mai.append("01").append(String.format("%02d", acquirerData.length())).append(acquirerData);
            String serviceCode = "QRIBFTTA";
            mai.append("02").append(String.format("%02d", serviceCode.length())).append(serviceCode);
            
            String maiData = mai.toString();
            sb.append("38").append(String.format("%02d", maiData.length())).append(maiData);

            // 53: Transaction Currency
            sb.append("53").append("03").append("704");

            // 54: Transaction Amount
            String amountStr = amount.stripTrailingZeros().toPlainString();
            sb.append("54").append(String.format("%02d", amountStr.length())).append(amountStr);

            // 58: Country Code
            sb.append("58").append("02").append("VN");

            // 62: Additional Data Field - Subfield 08
            StringBuilder additionalData = new StringBuilder();
            additionalData.append("08").append(String.format("%02d", transactionContent.length())).append(transactionContent);
            
            String additionalDataStr = additionalData.toString();
            sb.append("62").append(String.format("%02d", additionalDataStr.length())).append(additionalDataStr);

            // 63: CRC16
            String dataWithoutCrc = sb.toString() + "6304";
            String crc = calculateCRC16(dataWithoutCrc);
            sb.append("63").append("04").append(crc);

            return sb.toString();
        }
        
        private String calculateCRC16(String data) {
            int crc = 0xFFFF;
            
            for (byte b : data.getBytes()) {
                crc ^= (b & 0xFF);
                for (int i = 0; i < 8; i++) {
                    if ((crc & 1) != 0) {
                        crc = (crc >>> 1) ^ 0x8408;
                    } else {
                        crc >>>= 1;
                    }
                }
            }
            
            return String.format("%04X", (~crc) & 0xFFFF);
        }
    }
}
