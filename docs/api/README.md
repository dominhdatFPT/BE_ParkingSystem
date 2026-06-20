# API Documentation — Smart Parking Backend

> Tài liệu tập hợp toàn bộ REST API của backend.  
> Được tổ chức theo từng module để dễ tra cứu và bảo trì.

## Thông tin chung

- **Base URL:** `http://localhost:8080/api/v1`
- **Authentication:** JWT Bearer token
  - Header: `Authorization: Bearer <token>`
  - Token được lấy từ các API `/api/v1/auth/login` hoặc `/api/v1/auth/google-login`
- **CORS:** Cho phép `localhost:5173` (Vite frontend), `localhost:8080`, `127.0.0.1`
- **Response format:**
  - Phần lớn controller trả về `ResponseEntity<T>` trực tiếp.
  - Một số module (`vehicle-registrations`, `fee-subscriptions`, `customer/parking-orders`) sử dụng wrapper `ApiResponse<T>`.

## Role & phân quyền

| Role | Mô tả |
|------|-------|
| `USER` | Ngườii dùng thông thường (mặc định nếu không phải nhân viên) |
| `STAFF` | Nhân viên bãi xe |
| `ADMIN` | Quản trị viên |

> Lưu ý: Role được resolve động tại login qua bảng `employees`, không lưu trong bảng `users`.

## Mục lục API theo module

| File | Module | Mô tả |
|------|--------|-------|
| [01-auth.md](./01-auth.md) | Auth | Đăng nhập, đăng ký, Google login |
| [02-users.md](./02-users.md) | Users | Quản lý ngườii dùng |
| [03-bookings.md](./03-bookings.md) | Bookings | Đặt chỗ / booking |
| [04-parking.md](./04-parking.md) | Parking | Bãi đỗ, tầng, slot, bản đồ bãi xe |
| [05-vehicles.md](./05-vehicles.md) | Vehicle Registrations | Đăng ký phương tiện |
| [06-fee-subscriptions.md](./06-fee-subscriptions.md) | Fee Subscriptions | Gói phí & đăng ký gói |
| [07-staff-operations.md](./07-staff-operations.md) | Staff Operations | Nghiệp vụ nhân viên: check-in, duyệt booking, dashboard |
| [08-customer-orders.md](./08-customer-orders.md) | Customer Orders | Đơn đỗ xe đang hoạt động của khách |

## Danh sách API tổng hợp

### Public APIs (không cần JWT)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| `POST`   | `/api/v1/auth/login`        | Đăng nhập bằng email/password |
| `POST`   | `/api/v1/auth/register`     | Đăng ký tài khoản |
| `POST`   | `/api/v1/auth/signup`       | Alias của `/register` |
| `POST`   | `/api/v1/auth/google-login` | Đăng nhập bằng Google |
| `GET`    | `/api/v1/parking-area-summary/options` | Lấy option lọc khu vực |
| `GET`    | `/api/v1/parking-area-summary` | Tổng hợp khu vực đỗ xe |
| `GET`    | `/api/v1/fee-packages` | Danh sách gói phí |

### Authenticated APIs (cần JWT)

| Module | Method | Endpoint | Role |
|--------|--------|----------|------|
| Users | `GET`    | `/api/v1/users` | Bất kỳ user đăng nhập |
| Users | `GET`    | `/api/v1/users/me` | Bất kỳ user đăng nhập |
| Users | `GET`    | `/api/v1/users/{id}` | Bất kỳ user đăng nhập |
| Users | `POST`   | `/api/v1/users` | Bất kỳ user đăng nhập |
| Users | `PUT`    | `/api/v1/users/{id}` | Bất kỳ user đăng nhập |
| Users | `DELETE` | `/api/v1/users/{id}` | Bất kỳ user đăng nhập |
| Bookings | `GET`    | `/api/v1/bookings` | `ADMIN`, `STAFF` |
| Bookings | `GET`    | `/api/v1/bookings/{id}` | `ADMIN`, `STAFF` |
| Bookings | `GET`    | `/api/v1/bookings/my-bookings` | Authenticated |
| Bookings | `POST`   | `/api/v1/bookings` | Authenticated |
| Bookings | `POST`   | `/api/v1/bookings/admin-create` | `ADMIN`, `STAFF` |
| Bookings | `POST`   | `/api/v1/bookings/{id}/payment` | Authenticated |
| Bookings | `PUT`    | `/api/v1/bookings/{id}` | `ADMIN`, `STAFF` |
| Bookings | `DELETE` | `/api/v1/bookings/{id}` | `ADMIN`, `STAFF` |
| Parking | `GET`    | `/api/v1/parking-slots` | Authenticated |
| Parking | `GET`    | `/api/v1/parking-slots/{id}` | Authenticated |
| Parking | `POST`   | `/api/v1/parking-slots` | Authenticated |
| Parking | `PUT`    | `/api/v1/parking-slots/{id}` | Authenticated |
| Parking | `DELETE` | `/api/v1/parking-slots/{id}` | Authenticated |
| Parking | `GET`    | `/api/v1/parking-map/facility/{parkingId}` | Authenticated |
| Vehicles | `POST`   | `/api/v1/vehicle-registrations` | Authenticated |
| Vehicles | `GET`    | `/api/v1/vehicle-registrations/my` | Authenticated |
| Vehicles | `GET`    | `/api/v1/vehicle-registrations/pending` | `ADMIN`, `STAFF` |
| Vehicles | `GET`    | `/api/v1/vehicle-registrations` | `ADMIN`, `STAFF` |
| Vehicles | `GET`    | `/api/v1/vehicle-registrations/{id}` | Authenticated |
| Vehicles | `PUT`    | `/api/v1/vehicle-registrations/{id}/review` | `ADMIN`, `STAFF` |
| Fee Subscriptions | `GET`    | `/api/v1/fee-subscriptions/my-vehicles` | Authenticated |
| Fee Subscriptions | `POST`   | `/api/v1/fee-subscriptions` | Authenticated |
| Fee Subscriptions | `PATCH`  | `/api/v1/fee-subscriptions/{id}/cancel` | Authenticated |
| Staff Operations | `POST`   | `/api/v1/parking-entry/check` | `ADMIN`, `STAFF` |
| Staff Operations | `POST`   | `/api/v1/parking-entry/confirm` | `ADMIN`, `STAFF` |
| Staff Operations | `GET`    | `/api/v1/staff/bookings/pending` | `ADMIN`, `STAFF` |
| Staff Operations | `PATCH`  | `/api/v1/staff/bookings/{id}/approve` | `ADMIN`, `STAFF` |
| Staff Operations | `PATCH`  | `/api/v1/staff/bookings/{id}/reject` | `ADMIN`, `STAFF` |
| Staff Operations | `GET`    | `/api/v1/staff/operations-dashboard` | Authenticated |
| Staff Operations | `GET`    | `/api/v1/staff/parking-operations` | Authenticated |
| Staff Operations | `GET`    | `/api/v1/staff/parking-sessions` | Authenticated |
| Customer Orders | `GET`    | `/api/customer/parking-orders/active` | Authenticated |

## Ghi chú quan trọng cho developer

1. **Đường dẫn không đồng nhất:**
   - Hầu hết API dùng prefix `/api/v1`.
   - `CustomerParkingOrderController` đang dùng `/api/customer/parking-orders/active` (thiếu `/v1`).
   - Cần review nếu muốn thống nhất toàn bộ API về `/api/v1/...`.

2. **Phân quyền chưa đồng đều:**
   - `ParkingSlotController` và `UserController` hiện chỉ yêu cầu `authenticated`, chưa giới hạn `ADMIN`/`STAFF`.
   - `StaffOperationsController` cũng chưa explicit kiểm tra role dù đặt tên là `/staff`.

3. **Response format:**
   - Một số API trả về `ResponseEntity<T>` trực tiếp.
   - Một số API trả về `ApiResponse<T>` wrapper.
   - Nên thống nhất format response cho toàn bộ hệ thống.

## Cách cập nhật tài liệu này

Khi thêm/sửa/xóa API, hãy cập nhật đồng thờii:
1. File module tương ứng trong `docs/api/`
2. Bảng tổng hợp trong `docs/api/README.md`
3. Nếu thay đổi quyền truy cập, ghi chú vào phần "Ghi chú quan trọng"
