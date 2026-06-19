# Staff Operations API

> Các nghiệp vụ dành cho nhân viên/quản trị: check-in xe, duyệt booking, dashboard vận hành.

## Endpoints

### Parking Entry

| Method | Endpoint | Mô tả | Auth | Request DTO | Response DTO |
|--------|----------|-------|------|-------------|--------------|
| `POST` | `/api/v1/parking-entry/check` | Kiểm tra xe trước khi vào bãi | `ADMIN`, `STAFF` | `ParkingEntryCheckRequest` | `ParkingEntryResponse` |
| `POST` | `/api/v1/parking-entry/confirm` | Xác nhận xe vào bãi | `ADMIN`, `STAFF` | `ParkingEntryConfirmRequest` | `ParkingEntryResponse` |

### Staff Booking Approval

| Method | Endpoint | Mô tả | Auth | Request DTO |
|--------|----------|-------|------|-------------|
| `GET` | `/api/v1/staff/bookings/pending` | Lấy bookings chờ duyệt | `ADMIN`, `STAFF` | - |
| `PATCH` | `/api/v1/staff/bookings/{id}/approve` | Duyệt booking | `ADMIN`, `STAFF` | `StaffBookingDecisionRequest` (optional) |
| `PATCH` | `/api/v1/staff/bookings/{id}/reject` | Từ chối booking | `ADMIN`, `STAFF` | `StaffBookingDecisionRequest` (optional) |

### Operations Dashboard

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/v1/staff/operations-dashboard` | Dashboard tổng quan | Authenticated |
| `GET` | `/api/v1/staff/parking-operations` | Thông tin vận hành bãi xe | Authenticated |
| `GET` | `/api/v1/staff/parking-sessions` | Danh sách phiên đỗ xe đang hoạt động | Authenticated |

## Ghi chú

- Các endpoint `/api/v1/staff/**` hiện tại chỉ yêu cầu `authenticated`, chưa explicit kiểm tra role ở controller. Nếu muốn giới hạn chỉ STAFF/ADMIN, cần bổ sung annotation hoặc kiểm tra trong service.

## File liên quan

- Controllers:
  - `src/main/java/com/java/parking/controller/ParkingEntryController.java`
  - `src/main/java/com/java/parking/controller/StaffBookingController.java`
  - `src/main/java/com/java/parking/controller/StaffOperationsController.java`
- Services:
  - `src/main/java/com/java/parking/service/ParkingEntryService.java`
  - `src/main/java/com/java/parking/service/BookingService.java`
  - `src/main/java/com/java/parking/service/OperationsDashboardService.java`
- DTO Request: `ParkingEntryCheckRequest.java`, `ParkingEntryConfirmRequest.java`, `StaffBookingDecisionRequest.java`
- DTO Response: `ParkingEntryResponse.java`, `BookingResponse.java`, `OperationsDashboardResponse.java`, `ParkingOperationsResponse.java`
