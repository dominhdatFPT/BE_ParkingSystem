# Staff Operations

Các API vận hành dành cho `ADMIN` và `STAFF`.

| Method | Endpoint | Chức năng |
|--------|----------|-----------|
| `POST` | `/api/v1/parking-entry/check` | Kiểm tra phương tiện/thẻ xe trước khi vào |
| `POST` | `/api/v1/parking-entry/confirm` | Xác nhận xe vào |
| `POST` | `/api/v1/parking-exit/check` | Kiểm tra phiên gửi xe trước khi ra |
| `POST` | `/api/v1/parking-exit/{orderId}/confirm` | Xác nhận xe ra |
| `GET` | `/api/v1/staff/operations-dashboard` | Dashboard vận hành |
| `GET` | `/api/v1/staff/parking-operations` | Cấu trúc bãi, tầng và slot |
| `GET` | `/api/v1/staff/parking-sessions` | Danh sách phiên gửi xe |
| `GET` | `/api/v1/vehicle-registrations` | Danh sách hồ sơ đăng ký xe |
| `PUT` | `/api/v1/vehicle-registrations/{id}/review` | Duyệt hoặc từ chối hồ sơ |

Module staff không còn xử lý Booking. Trang quản lý gói hiện xét duyệt trực tiếp
`vehicle_registrations`.
