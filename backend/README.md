# Zettix Backend API

Hệ thống backend cho website bán tài khoản và key bản quyền với tính năng tự động hóa toàn bộ quy trình từ nạp tiền đến giao sản phẩm.

## Tính năng chính

### Khách hàng (User)
- Đăng ký, đăng nhập, quản lý thông tin cá nhân
- Ví nội bộ (Wallet Balance)
- Nạp tiền qua VietQR với xác nhận tự động qua Gmail API
- Mua sản phẩm và nhận tài khoản/key trực tiếp trên web
- Xem lịch sử giao dịch và mua hàng

### Quản trị viên (Admin)
- Quản lý người dùng (khóa/mở tài khoản)
- Quản lý sản phẩm (CRUD, giá bán, bật/tắt)
- Quản lý kho tài khoản/key
- Quản lý đơn hàng và giao dịch
- Dashboard thống kê

## Công nghệ sử dụng

- **Java 17**
- **Spring Boot 3** (Web, Data JPA, Security, Validation, Quartz)
- **MySQL** - Cơ sở dữ liệu
- **JWT Authentication** - Xác thực và phân quyền
- **Gmail API (OAuth2)** - Đọc email giao dịch từ Vietcombank
- **VietQR (EMVCo)** - Tạo QR code thanh toán
- **Swagger/OpenAPI** - Tài liệu API

## Cài đặt và chạy

### Yêu cầu hệ thống
- Java 17+
- MySQL 8.0+
- Maven 3.6+

### Cấu hình cơ sở dữ liệu

1. Tạo database:
```sql
CREATE DATABASE zettix_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Cấu hình trong `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zettix_db
    username: your_username
    password: your_password
```

### Chạy ứng dụng

1. Clone repository:
```bash
git clone <repository-url>
cd zettix-backend
```

2. Cài đặt dependencies:
```bash
mvn clean install
```

3. Chạy ứng dụng:
```bash
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: http://localhost:8083

## API Documentation

### Swagger UI
Truy cập: http://localhost:8083/api/swagger-ui.html

### Các endpoint chính

#### Authentication
- `POST /api/auth/register` - Đăng ký user mới
- `POST /api/auth/login` - Đăng nhập
- `GET /api/auth/me` - Lấy thông tin user hiện tại

#### User APIs
- `POST /api/user/deposit` - Tạo giao dịch nạp tiền
- `GET /api/user/transactions` - Lịch sử giao dịch
- `POST /api/user/purchase` - Mua sản phẩm
- `GET /api/user/products` - Danh sách sản phẩm

#### Admin APIs
- `GET /api/admin/users` - Quản lý users
- `POST /api/admin/products` - Quản lý sản phẩm
- `GET /api/admin/dashboard` - Dashboard thống kê

#### Public APIs
- `GET /api/public/products` - Danh sách sản phẩm công khai
- `GET /api/public/health` - Health check

## Cấu hình Gmail API

1. Tạo project trên Google Cloud Console
2. Bật Gmail API
3. Tạo OAuth 2.0 credentials
4. Tải file credentials và đặt tại `gmail-credentials.json`
5. Cấu hình trong `application.yml`:
```yaml
gmail:
  credentials-file: gmail-credentials.json
  query: "from:VCBDigibank@info.vietcombank.com.vn subject:Thông báo giao dịch"
```

## Cấu hình VietQR

Cấu hình thông tin ngân hàng trong `application.yml`:
```yaml
vietqr:
  bank-code: 970436
  account-number: 9889559357
  account-name: NGUYEN VAN HOANG
```

## Bảo mật

- JWT Authentication với token hết hạn 24h
- Password được mã hóa bằng BCrypt
- Phân quyền User/Admin
- CORS được cấu hình cho frontend
- Validation đầu vào cho tất cả API

## Monitoring và Logging

- Actuator endpoints tại `/api/actuator`
- Logging với Logback
- Quartz scheduler cho các tác vụ định kỳ

## Docker Support

### Chạy với Docker Compose

```bash
docker-compose up -d
```

### Build Docker image

```bash
docker build -t zettix-backend .
```

## Troubleshooting

### Lỗi kết nối database
- Kiểm tra MySQL đang chạy
- Kiểm tra thông tin kết nối trong `application.yml`

### Lỗi Gmail API
- Kiểm tra file `gmail-credentials.json`
- Kiểm tra OAuth credentials đã được cấu hình đúng

### Lỗi VietQR
- Kiểm tra thông tin ngân hàng trong `application.yml`
- Kiểm tra format QR code

## License

MIT License