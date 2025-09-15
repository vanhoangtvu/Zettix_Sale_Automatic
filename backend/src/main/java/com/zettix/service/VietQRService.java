package com.zettix.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VietQRService {

    @Value("${vietqr.bank-code}")
    private String bankCode; // BIN ngân hàng, ví dụ 970436 = VCB

    @Value("${vietqr.account-number}")
    private String accountNumber;

    @Value("${vietqr.account-name}")
    private String accountName;

    @Value("${vietqr.qr-width:300}")
    private int qrWidth;

    @Value("${vietqr.qr-height:300}")
    private int qrHeight;

    @Value("${business.qr-expiration-minutes:15}")
    private int qrExpirationMinutes;

    @Value("${vietqr.merchant-city:HO CHI MINH}")
    private String merchantCity;

    public VietQRResponse generateQRCode(BigDecimal amount, String referenceId) {
        try {
            String payload = buildVietQRPayload(amount, referenceId);
            String b64 = generateQRCodeImage(payload);
            return VietQRResponse.builder()
                    .qrData(payload)
                    .qrCodeBase64(b64)
                    .amount(amount)
                    .referenceId(referenceId)
                    .accountNumber(accountNumber)
                    .accountName(accountName)
                    .bankCode(bankCode)
                    .expiresAt(LocalDateTime.now().plusMinutes(qrExpirationMinutes))
                    .build();
        } catch (Exception e) {
            log.error("Generate VietQR failed", e);
            throw new RuntimeException("Failed to generate QR", e);
        }
    }

    /** Build EMVCo VietQR payload theo đúng chuẩn thực tế. */
    private String buildVietQRPayload(BigDecimal amount, String referenceId) {
        if (bankCode == null || bankCode.isEmpty()) throw new IllegalArgumentException("bankCode is empty");
        if (accountNumber == null || accountNumber.isEmpty()) throw new IllegalArgumentException("accountNumber is empty");
        if (accountName == null || accountName.isEmpty()) throw new IllegalArgumentException("accountName is empty");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("amount must > 0");
        if (referenceId == null || referenceId.isEmpty()) throw new IllegalArgumentException("referenceId is empty");

        // Clean referenceId: chỉ chữ + số in hoa
        String cleanRef = referenceId.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        
        // Tạo nội dung chuyển khoản theo chuẩn thực tế
        String transactionContent = "NAP TIEN ZETTIX " + cleanRef;

        StringBuilder sb = new StringBuilder();

        // 00: Payload Format Indicator
        sb.append("00").append("02").append("01");

        // 01: Point of Initiation Method (12 = static để khóa thông tin)
        sb.append("01").append("02").append("12");

        // 38: Merchant Account Information theo chuẩn thực tế
        StringBuilder mai = new StringBuilder();
        
        // 00: Globally Unique Identifier
        mai.append("00").append("10").append("A000000727");
        
        // 01: Acquirer ID + Account Number (theo format thực tế)
        String acquirerData = "0006" + bankCode + "0110" + accountNumber;
        mai.append("01").append(String.format("%02d", acquirerData.length())).append(acquirerData);
        
        // 02: Service Code (theo chuẩn VietQR thực tế)
        String serviceCode = "QRIBFTTA"; // QR Instant Bank Transfer to Account
        mai.append("02").append(String.format("%02d", serviceCode.length())).append(serviceCode);
        
        String maiData = mai.toString();
        sb.append("38").append(String.format("%02d", maiData.length())).append(maiData);

        // 53: Transaction Currency (704 = VND)
        sb.append("53").append("03").append("704");

        // 54: Transaction Amount
        String amountStr = amount.stripTrailingZeros().toPlainString();
        sb.append("54").append(String.format("%02d", amountStr.length())).append(amountStr);

        // 58: Country Code
        sb.append("58").append("02").append("VN");

        // 62: Additional Data Field - Nội dung chuyển khoản (QUAN TRỌNG)
        StringBuilder additionalData = new StringBuilder();
        
        // 08: Purpose of Transaction - Nội dung hiển thị trong app ngân hàng
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
        for (int i = 0; i < data.length(); i++) {
            crc ^= (data.charAt(i) & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 0x8408;
                } else {
                    crc >>= 1;
                }
            }
        }
        crc ^= 0xFFFF;
        return String.format("%04X", crc & 0xFFFF);
    }

    private String generateQRCodeImage(String data) throws WriterException, IOException {
        QRCodeWriter qr = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = qr.encode(data, BarcodeFormat.QR_CODE, qrWidth, qrHeight, hints);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", os);
        return Base64.getEncoder().encodeToString(os.toByteArray());
    }

    public String generateReferenceId() {
        return "ZETTIX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @lombok.Data
    @lombok.Builder
    public static class VietQRResponse {
        private String qrData;
        private String qrCodeBase64;
        private BigDecimal amount;
        private String referenceId;
        private String accountNumber;
        private String accountName;
        private String bankCode;
        private LocalDateTime expiresAt;
    }
}
