# Bookings API

> Quản lý đặt chỗ đỗ xe (booking).

## Endpoints

| Method | Endpoint | Mô tả | Auth | Request DTO | Response DTO |
|--------|----------|-------|------|-------------|--------------|
| `GET` | `/api/v1/bookings` | Lấy tất cả bookings | `ADMIN`, `STAFF` | - | `List<BookingResponse>` |
| `GET` | `/api/v1/bookings/{id}` | Lấy booking theo ID | `ADMIN`, `STAFF` | - | `BookingResponse` |
| `GET` | `/api/v1/bookings/my-bookings` | Lấy bookings của user hiện tại | Authenticated | - | `List<BookingResponse>` |
| `POST` | `/api/v1/bookings` | Tạo booking (user) | Authenticated | `UserBookingRequest` | `BookingResponse` |
| `POST` | `/api/v1/bookings/admin-create` | Tạo booking (admin/staff) | `ADMIN`, `STAFF` | `BookingRequest` | `BookingResponse` |
| `POST` | `/api/v1/bookings/{id}/payment` | Thanh toán booking | Authenticated | `BookingPaymentRequest` | `BookingResponse` |
| `PUT` | `/api/v1/bookings/{id}` | Cập nhật booking | `ADMIN`, `STAFF` | `BookingRequest` | `BookingResponse` |
| `DELETE` | `/api/v1/bookings/{id}` | Xóa booking | `ADMIN`, `STAFF` | - | `Void` |

## Chi tiết

### `GET /api/v1/bookings/my-bookings`

Lấy danh sách booking của user đang đăng nhập.

### `POST /api/v1/bookings`

User tự tạo booking cho chính mình.

### `POST /api/v1/bookings/admin-create`

Admin/Staff tạo booking thay user.

### `POST /api/v1/bookings/{id}/payment`

Request body có thể null (`required = false`).

## File liên quan

- Controller: `src/main/java/com/java/parking/controller/BookingController.java`
- Service: `src/main/java/com/java/parking/service/BookingService.java`
- DTO Request: `src/main/java/com/java/parking/dto/request/BookingRequest.java`, `UserBookingRequest.java`, `BookingPaymentRequest.java`
- DTO Response: `src/main/java/com/java/parking/dto/response/BookingResponse.java`
