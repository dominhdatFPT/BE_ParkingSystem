# API Smart Parking Backend

Backend hiện tập trung vào các chức năng: đăng ký thẻ xe/eKYC, mua biểu phí,
thanh toán, thông báo, hỗ trợ và vận hành xe vào/ra.

## Tài liệu theo module

| File | Nội dung |
|------|----------|
| [01-auth.md](./01-auth.md) | Đăng nhập, đăng ký tài khoản và Google login |
| [02-users.md](./02-users.md) | Quản lý người dùng |
| [04-parking.md](./04-parking.md) | Slot, bãi xe và dữ liệu vận hành |
| [05-vehicles.md](./05-vehicles.md) | Đăng ký thẻ xe, eKYC và xét duyệt hồ sơ |
| [06-fee-subscriptions.md](./06-fee-subscriptions.md) | Biểu phí, đăng ký gói và hóa đơn |
| [07-staff-operations.md](./07-staff-operations.md) | Xe vào, xe ra, phiên gửi xe và dashboard |
| [09-system-notifications-payments.md](./09-system-notifications-payments.md) | Thông báo, hỗ trợ và MoMo |

## Endpoint chính dành cho người dùng

| Method | Endpoint | Chức năng |
|--------|----------|-----------|
| `POST` | `/api/v1/vehicle-registrations` | Gửi hồ sơ đăng ký thẻ xe |
| `GET` | `/api/v1/vehicle-registrations/my` | Hồ sơ đăng ký của người dùng |
| `GET` | `/api/v1/fee-packages` | Danh sách biểu phí |
| `GET` | `/api/v1/fee-subscriptions/my-vehicles` | Xe có thể mua gói |
| `POST` | `/api/subscriptions/register` | Tạo đăng ký gói và đơn MoMo |
| `GET` | `/api/subscriptions/my` | Danh sách gói đã đăng ký |
| `GET` | `/api/subscriptions/my-invoices` | Lịch sử hóa đơn |
| `GET` | `/api/customer/notifications` | Thông báo của người dùng |
| `POST` | `/api/customer/support` | Gửi yêu cầu hỗ trợ |

## Lưu ý

- API cần JWT dùng header `Authorization: Bearer <token>`.
- Principal trong backend là `Long userId`.
- Module Booking và API bản đồ bãi xe cũ đã được loại khỏi source.
- Giao diện trợ lý đăng ký xe dùng trực tiếp API `vehicle-registrations`; không còn
  module Gemini chat riêng.
