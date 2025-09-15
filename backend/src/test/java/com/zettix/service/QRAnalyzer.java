package com.zettix.service;

public class QRAnalyzer {
    
    public static void main(String[] args) {
        String qrData = "00020101021238540010A00000072701240006970436011098895593570208QRIBFTTA53037045405100005802VN62320828day la noi dung chuyen khoan6304EEF5";
        
        System.out.println("=== Phân tích QR Code chuẩn ===");
        System.out.println("QR Data: " + qrData);
        System.out.println();
        
        analyzeQRCode(qrData);
    }
    
    public static void analyzeQRCode(String qrData) {
        int index = 0;
        
        while (index < qrData.length() - 4) {
            try {
                String tag = qrData.substring(index, index + 2);
                String lengthStr = qrData.substring(index + 2, index + 4);
                int length = Integer.parseInt(lengthStr);
                
                if (index + 4 + length > qrData.length()) break;
                
                String value = qrData.substring(index + 4, index + 4 + length);
                
                System.out.println("Field " + tag + " (Length: " + length + "): " + value);
                
                // Phân tích sub-fields cho field 38 và 62
                if ("38".equals(tag)) {
                    System.out.println("  --> Merchant Account Info:");
                    analyzeSubFields(value, "    ");
                } else if ("62".equals(tag)) {
                    System.out.println("  --> Additional Data:");
                    analyzeSubFields(value, "    ");
                }
                
                index += 4 + length;
                
            } catch (Exception e) {
                break;
            }
        }
    }
    
    private static void analyzeSubFields(String data, String indent) {
        int index = 0;
        
        while (index < data.length() - 4) {
            try {
                String tag = data.substring(index, index + 2);
                String lengthStr = data.substring(index + 2, index + 4);
                int length = Integer.parseInt(lengthStr);
                
                if (index + 4 + length > data.length()) break;
                
                String value = data.substring(index + 4, index + 4 + length);
                
                System.out.println(indent + "Subfield " + tag + " (Length: " + length + "): " + value);
                
                index += 4 + length;
                
            } catch (Exception e) {
                break;
            }
        }
    }
}
