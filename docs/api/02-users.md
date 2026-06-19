# Users API

> Quản lý ngườii dùng.  
> Hiện tại tất cả endpoint chỉ yêu cầu đăng nhập (`authenticated`), chưa phân biệt role.

## Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/v1/users` | Lấy danh sách tất cả users | Authenticated |
| `GET` | `/api/v1/users/me` | Lấy thông tin user hiện tại | Authenticated |
| `GET` | `/api/v1/users/{id}` | Lấy thông tin user theo ID | Authenticated |
| `POST` | `/api/v1/users` | Tạo user mới | Authenticated |
| `PUT` | `/api/v1/users/{id}` | Cập nhật user | Authenticated |
| `DELETE` | `/api/v1/users/{id}` | Xóa user | Authenticated |

## Chi tiết

### `GET /api/v1/users/me`

Lấy thông tin user từ JWT token hiện tại.

### `POST /api/v1/users`

Request body sử dụng entity `User` trực tiếp.

## File liên quan

- Controller: `src/main/java/com/java/parking/controller/UserController.java`
- Service: `src/main/java/com/java/parking/service/UserService.java`
- DTO Response: `src/main/java/com/java/parking/dto/response/UserResponse.java`
- Entity: `src/main/java/com/java/parking/model/User.java`
