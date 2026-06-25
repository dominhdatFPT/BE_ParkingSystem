# Auth API

> Tất cả API auth đều **public**, không cần JWT.

## Endpoints

| Method | Endpoint | Mô tả | Request DTO | Response DTO |
|--------|----------|-------|-------------|--------------|
| `POST` | `/api/v1/auth/login` | Đăng nhập bằng email/password | `LoginRequest` | `AuthResponse` |
| `POST` | `/api/v1/auth/register` | Đăng ký tài khoản mới | `RegisterRequest` | `AuthResponse` |
| `POST` | `/api/v1/auth/signup` | Alias của `/register` | `RegisterRequest` | `AuthResponse` |
| `POST` | `/api/v1/auth/google-login` | Đăng nhập bằng Google token | `GoogleLoginRequest` | `AuthResponse` |

## Chi tiết

### `POST /api/v1/auth/login`

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "string"
}
```

**Response:** `AuthResponse` chứa JWT token và thông tin user.

### `POST /api/v1/auth/register` (và `/signup`)

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "string",
  "fullName": "string",
  "phone": "string"
}
```

> Cấu trúc chính xác xem `RegisterRequest`.

### `POST /api/v1/auth/google-login`

**Request body:**
```json
{
  "idToken": "string"
}
```

> Cấu trúc chính xác xem `GoogleLoginRequest`.

## File liên quan

- Controller: `src/main/java/com/swp/parking/controller/AuthController.java`
- Service: `src/main/java/com/swp/parking/service/AuthService.java`
- DTO Request: `src/main/java/com/swp/parking/dto/request/LoginRequest.java`, `RegisterRequest.java`, `GoogleLoginRequest.java`
- DTO Response: `src/main/java/com/swp/parking/dto/response/AuthResponse.java`
