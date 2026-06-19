# Vehicle Registrations API

> Đăng ký phương tiện của khách hàng. Có flow duyệt bởi ADMIN/STAFF.

## Endpoints

| Method | Endpoint | Mô tả | Auth | Request DTO | Response |
|--------|----------|-------|------|-------------|----------|
| `POST` | `/api/v1/vehicle-registrations` | Tạo đăng ký xe mới | Authenticated | `VehicleRegistrationRequest` | `ApiResponse<VehicleRegistrationResponse>` |
| `GET` | `/api/v1/vehicle-registrations/my` | Lấy đăng ký xe của user hiện tại | Authenticated | - | `ApiResponse<List<VehicleRegistrationResponse>>` |
| `GET` | `/api/v1/vehicle-registrations/pending` | Lấy danh sách chờ duyệt | `ADMIN`, `STAFF` | `page`, `size` | `ApiResponse<Page<VehicleRegistrationResponse>>` |
| `GET` | `/api/v1/vehicle-registrations` | Lấy tất cả đăng ký xe | `ADMIN`, `STAFF` | `status`, `page`, `size` | `ApiResponse<Page<VehicleRegistrationResponse>>` |
| `GET` | `/api/v1/vehicle-registrations/{id}` | Lấy chi tiết đăng ký xe | Authenticated | - | `ApiResponse<VehicleRegistrationResponse>` |
| `PUT` | `/api/v1/vehicle-registrations/{id}/review` | Duyệt/từ chối đăng ký | `ADMIN`, `STAFF` | `AdminReviewRequest` | `ApiResponse<VehicleRegistrationResponse>` |

## Chi tiết

### `GET /api/v1/vehicle-registrations`

**Query params:**
- `status` (optional): ví dụ PENDING, APPROVED, REJECTED
- `page` (default: 0)
- `size` (default: 50)

### `GET /api/v1/vehicle-registrations/pending`

**Query params:**
- `page` (default: 0)
- `size` (default: 10)

### `PUT /api/v1/vehicle-registrations/{id}/review`

Request body `AdminReviewRequest` chứa thông tin quyết định duyệt/từ chối.

## File liên quan

- Controller: `src/main/java/com/java/parking/controller/VehicleRegistrationController.java`
- Service: `src/main/java/com/java/parking/service/VehicleRegistrationService.java`
- DTO Request: `VehicleRegistrationRequest.java`, `AdminReviewRequest.java`
- DTO Response: `VehicleRegistrationResponse.java`
