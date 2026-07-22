# Users API (Deprecated)

> **Đã gộp vào AccountController.** Xem `docs/api/` — file tài liệu AccountController.
>
> File `UserController.java`, `UserService.java`, `UserResponse.java` đã bị xóa.
> Tất cả endpoint CRUD user hiện nằm tại `/api/v1/admin/accounts/...`.

## Endpoints cũ (không còn tồn tại)

| Method | Endpoint | Thay thế bởi |
|--------|----------|--------------|
| `GET /api/v1/users` | Danh sách users | `GET /api/v1/admin/accounts/users` |
| `GET /api/v1/users/me` | User hiện tại | JWT claims + `/api/v1/profile/*` |
| `GET /api/v1/users/{id}` | Chi tiết user | `GET /api/v1/admin/accounts/{userId}` |
| `POST /api/v1/users` | Tạo user | `POST /api/v1/admin/accounts` |
| `PUT /api/v1/users/{id}` | Cập nhật user | `PATCH /api/v1/admin/accounts/{userId}/lock\|unlock` |
| `DELETE /api/v1/users/{id}` | Xóa user | (chưa có tương đương an toàn) |
