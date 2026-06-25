# Users API

> Quản lý người dùng.

## Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/v1/users` | Lấy danh sách tất cả users | `ADMIN`, `STAFF` |
| `GET` | `/api/v1/users/me` | Lấy thông tin user hiện tại | Authenticated |
| `GET` | `/api/v1/users/{id}` | Lấy thông tin user theo ID | `ADMIN`, `STAFF` |
| `POST` | `/api/v1/users` | Tạo user mới | `ADMIN`, `STAFF` |
| `PUT` | `/api/v1/users/{id}` | Cập nhật user | `ADMIN`, `STAFF` |
| `DELETE` | `/api/v1/users/{id}` | Xóa user | `ADMIN`, `STAFF` |

## Chi tiết

### `GET /api/v1/users/me`

Lấy thông tin user từ JWT token hiện tại.

### `POST /api/v1/users`

Request body sử dụng entity `User` trực tiếp.

## File liên quan

- Controller: `src/main/java/com/swp/parking/controller/UserController.java`
- Service: `src/main/java/com/swp/parking/service/UserService.java`
- DTO Response: `src/main/java/com/swp/parking/dto/response/UserResponse.java`
- Entity: `src/main/java/com/swp/parking/model/User.java`
