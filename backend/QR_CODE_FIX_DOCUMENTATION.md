# Fix QR Code & Email Processing - Zettix Backend

## Vấn đề ban đầu
Khi người dùng quét QR code bằng app ngân hàng (VietCombank, MoMo, etc.), chỉ có số tiền hiển thị mà **không có nội dung chuyển khoản**, khiến:
1. Người dùng phải nhập tay nội dung chuyển khoản
2. Dễ nhập sai dẫn đến không match được với hệ thống
3. Email tự động không parse được đúng

## Nguyên nhân
1. **QR Code EMVCo format sai**: 
   - Chỉ có field `07` (Reference Label) cho technical reference
   - Thiếu field `05` (Bill Number) để hiển thị nội dung trong app ngân hàng

2. **Email parsing không robust**: 
   - Chỉ support 1 format email duy nhất
   - Không handle được các format khác của Vietcombank

3. **Reference matching không flexible**: 
   - Không clean reference khi match
   - Không handle trường hợp user nhập khác format

## Giải pháp đã triển khai

### 1. Sửa VietQRService.java
```java
// TRƯỚC: Chỉ có Reference Label  
String ref = "07" + String.format("%02d", cleanRef.length()) + cleanRef;
sb.append("62").append(String.format("%02d", ref.length())).append(ref);

// SAU: Có cả Bill Number và Reference Label
StringBuilder additionalData = new StringBuilder();
// 05: Bill Number - Hiển thị trong app ngân hàng
additionalData.append("05").append(String.format("%02d", description.length())).append(description);
// 07: Reference Label - Mã tham chiếu kỹ thuật  
additionalData.append("07").append(String.format("%02d", cleanRef.length())).append(cleanRef);
```

**Kết quả**: 
- App ngân hàng hiển thị: `NAP TIEN ZETTIX ZETTIX123456789`
- User chỉ cần xác nhận, không cần nhập tay

### 2. Cải thiện GmailService.java  
```java
// TRƯỚC: Chỉ 1 pattern cứng nhắc
Pattern referencePattern = Pattern.compile("Nội dung: (.+)");

// SAU: Support nhiều format
Pattern[] referencePatterns = {
    Pattern.compile("Nội dung\\s*:\\s*(.+?)(?=\\n|$)"),
    Pattern.compile("Diễn giải\\s*:\\s*(.+?)(?=\\n|$)"), 
    Pattern.compile("Memo\\s*:\\s*(.+?)(?=\\n|$)"),
    Pattern.compile("Content\\s*:\\s*(.+?)(?=\\n|$)"),
    Pattern.compile("NAP TIEN ZETTIX\\s+([A-Z0-9]+)")
};
```

**Kết quả**: 
- Parse được nhiều format email khác nhau
- Extract đúng ZETTIX reference code
- Clean reference khi match với database

### 3. Nâng cấp TransactionService.java
```java
// TRƯỚC: Throw exception khi không tìm thấy
Transaction transaction = transactionRepository.findByReferenceId(referenceId)
    .orElseThrow(() -> new RuntimeException("Transaction not found"));

// SAU: Return boolean, flexible matching
public boolean processDepositConfirmation(String referenceId, BigDecimal amount) {
    Transaction transaction = transactionRepository.findByReferenceId(referenceId).orElse(null);
    if (transaction == null) {
        log.warn("Transaction not found for reference: {}", referenceId);
        return false;
    }
    
    // Verify amount matches
    if (transaction.getAmount().compareTo(amount) != 0) {
        log.warn("Amount mismatch for transaction {}: expected {}, got {}", 
                referenceId, transaction.getAmount(), amount);
        return false;
    }
    
    // Process and return success status
    return true;
}
```

## Test Results

### QR Code Test
```
=== VietQR Test Result ===
Reference ID: ZETTIX1757873411140761542
Amount: 100000
QR Data: 00020101021138580010A000000727...62740541NAP TIEN ZETTIX...0725ZETTIX...
✓ Bill Number (05) found - will show content in banking app  
✓ Reference Label (07) found - for technical matching

=== Expected Behavior ===
• When scanned in banking app: Should show 'NAP TIEN ZETTIX ZETTIX1757873411140761542'
• User should enter exactly: NAP TIEN ZETTIX ZETTIX1757873411140761542  
• System will match using cleaned reference: ZETTIX1757873411140761542
```

### Email Parsing Test  
```
--- Email Sample 1 ---
Input: Tài khoản: 9889559357 | Số tiền: 100,000 VND | Nội dung: NAP TIEN ZETTIX ZETTIX123456
✓ Parse Success: true, Cleaned Reference: ZETTIX123456

--- Email Sample 2 ---  
Input: Số tài khoản: 9889559357 | Số tiền: 50,000 | Diễn giải: NAP TIEN ZETTIX ABC123
✓ Parse Success: true, Cleaned Reference: ABC123

--- Email Sample 3 ---
Input: TK: 9889559357 | +100,000 VND | Content: NAP TIEN ZETTIX XYZ789  
✓ Parse Success: true, Cleaned Reference: XYZ789

--- Email Sample 4 ---
Input: Account: 9889559357 | Amount: 200,000 | Memo: NAP TIEN ZETTIX DEF456
✓ Parse Success: true, Cleaned Reference: DEF456
```

## Luồng hoạt động mới

1. **User tạo giao dịch nạp tiền**
   - Hệ thống tạo referenceId: `ZETTIX1757873411140761542`
   - Tạo QR EMVCo với description: `NAP TIEN ZETTIX ZETTIX1757873411140761542`

2. **User quét QR bằng app ngân hàng**  
   - App tự động điền nội dung: `NAP TIEN ZETTIX ZETTIX1757873411140761542`
   - User chỉ cần xác nhận chuyển khoản

3. **Hệ thống nhận email Vietcombank**
   - Parse đa dạng format email  
   - Extract reference: `ZETTIX1757873411140761542`
   - Match với pending transaction
   - Tự động cộng tiền vào ví

## Deployment

1. Build và deploy backend:
```bash
mvn clean compile
mvn spring-boot:run
```

2. Test QR code với app ngân hàng thật

3. Monitor logs để verify email processing:
```bash
tail -f logs/spring.log | grep -E "(VietQR|Gmail|Transaction)"
```

## Notes quan trọng

- **EMVCo VietQR** hiện tại tuân thủ chuẩn NAPAS
- **Field 05** (Bill Number) là nội dung hiển thị trong app
- **Field 07** (Reference Label) là mã kỹ thuật để match
- **Gmail API** cần credentials hợp lệ trong `gmail-credentials.json`
- **Database** cần có bảng transactions với trường reference_id indexed

Với các thay đổi này, QR code sẽ hiển thị đầy đủ thông tin trong app ngân hàng và email processing sẽ hoạt động tự động.
