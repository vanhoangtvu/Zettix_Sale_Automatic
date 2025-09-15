package com.zettix.service;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

public class GmailServiceParsingTest {

    @Test
    public void testEmailParsing() {
        // Test email samples from different banks
        String[] emailSamples = {
            // Vietcombank format 1
            "Tài khoản: 9889559357\nSố tiền: 100,000 VND\nNội dung: NAP TIEN ZETTIX ZETTIX123456\nThời gian: 15/09/2025 01:30:00",
            
            // Vietcombank format 2  
            "Số tài khoản: 9889559357\nSố tiền: 50,000\nDiễn giải: NAP TIEN ZETTIX ABC123\nNgày: 15/09/2025 14:30:00",
            
            // Format with +/- signs
            "TK: 9889559357\n+100,000 VND\nContent: NAP TIEN ZETTIX XYZ789\nTime: 15/09/2025 08:45:00",
            
            // Messy format
            "Account:   9889559357  \nAmount: 200,000\nMemo:  NAP TIEN ZETTIX   DEF456  \n",
        };
        
        System.out.println("=== Gmail Service Parsing Test ===");
        
        for (int i = 0; i < emailSamples.length; i++) {
            System.out.println("\n--- Email Sample " + (i + 1) + " ---");
            System.out.println("Input: " + emailSamples[i].replace("\n", " | "));
            
            // Test patterns manually since we can't easily access private methods
            String body = emailSamples[i];
            
            // Account patterns
            String account = extractWithPatterns(body, new String[]{
                "Tài khoản\\s*:\\s*(\\d+)",
                "Số tài khoản\\s*:\\s*(\\d+)", 
                "TK\\s*:\\s*(\\d+)",
                "Account\\s*:\\s*(\\d+)"
            });
            
            // Amount patterns  
            String amount = extractWithPatterns(body, new String[]{
                "Số tiền\\s*:\\s*([\\d,]+)\\s*VND",
                "Số tiền\\s*:\\s*([\\d,]+)",
                "Amount\\s*:\\s*([\\d,]+)",
                "\\+(\\d[\\d,]*)\\s*VND"
            });
            
            // Reference patterns
            String reference = extractWithPatterns(body, new String[]{
                "Nội dung\\s*:\\s*(.+?)(?=\\n|$)",
                "Diễn giải\\s*:\\s*(.+?)(?=\\n|$)",
                "Memo\\s*:\\s*(.+?)(?=\\n|$)",
                "Content\\s*:\\s*(.+?)(?=\\n|$)"
            });
            
            // Extract ZETTIX reference
            String cleanRef = "";
            if (reference != null && !reference.isEmpty()) {
                java.util.regex.Pattern zettixPattern = java.util.regex.Pattern.compile("(?:NAP TIEN ZETTIX|ZETTIX)\\s*([A-Z0-9]+)");
                java.util.regex.Matcher zettixMatcher = zettixPattern.matcher(reference);
                if (zettixMatcher.find()) {
                    cleanRef = zettixMatcher.group(1);
                } else {
                    cleanRef = reference.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                }
            }
            
            System.out.println("Extracted Account: " + account);
            System.out.println("Extracted Amount: " + (amount != null ? amount.replace(",", "") : "null"));
            System.out.println("Extracted Reference: " + reference);
            System.out.println("Cleaned Reference: " + cleanRef);
            System.out.println("Parse Success: " + (account != null && amount != null));
        }
    }
    
    private String extractWithPatterns(String body, String[] patterns) {
        for (String patternStr : patterns) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
            java.util.regex.Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }
}
