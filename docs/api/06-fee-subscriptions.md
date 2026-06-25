# Fee Subscriptions API

> Quản lý gói phí (fee packages) và đăng ký gói phí (fee subscriptions).

## Endpoints

### Fee Packages (Public)

| Method | Endpoint | Mô tả | Auth | Query Params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/fee-packages` | Danh sách gói phí | Public | `category` hoặc `vehicleTypeId` |

### Fee Subscriptions (Authenticated)

| Method | Endpoint | Mô tả | Auth | Request DTO |
|--------|----------|-------|------|-------------|
| `GET` | `/api/v1/fee-subscriptions/my-vehicles` | Lấy xe của user để đăng ký gói | Authenticated | `category` hoặc `vehicleTypeId` |
| `POST` | `/api/v1/fee-subscriptions` | Tạo đăng ký gói mới | Authenticated | `CreateSubscriptionRequest` |
| `GET` | `/api/v1/fee-subscriptions/my` | Danh sách gói của user hiện tại | Authenticated | - |
| `POST` | `/api/v1/fee-subscriptions/{id}/payment` | Tạo/xử lý thanh toán gói | Authenticated | - |
| `PATCH` | `/api/v1/fee-subscriptions/{id}/cancel` | Hủy gói đã đăng ký | Authenticated | - |

## Chi tiết

### `GET /api/v1/fee-packages`

Trả về `ApiResponse<List<FeePackageResponse>>`.

### `POST /api/v1/fee-subscriptions`

Trả về `ApiResponse<CreateSubscriptionResponse>` với HTTP 201.

### `PATCH /api/v1/fee-subscriptions/{id}/cancel`

Trả về `ApiResponse<Void>` với message "Hủy gói thành công".

## File liên quan

- Controller: `src/main/java/com/swp/parking/controller/FeeSubscriptionController.java`
- Services:
  - `src/main/java/com/swp/parking/service/FeePackageService.java`
  - `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`
- DTO Request: `CreateSubscriptionRequest.java`
- DTO Response: `FeePackageResponse.java`, `CreateSubscriptionResponse.java`, `MyVehicleResponse.java`
