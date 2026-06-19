# Customer Orders API

> Các endpoint dành cho khách hàng xem đơn đỗ xe đang hoạt động.

## Endpoints

| Method | Endpoint | Mô tả | Auth | Response |
|--------|----------|-------|------|----------|
| `GET` | `/api/customer/parking-orders/active` | Lấy đơn đỗ xe đang hoạt động của user hiện tại | Authenticated | `ApiResponse<List<ActiveParkingOrderResponse>>` |

## Ghi chú quan trọng

- Endpoint này sử dụng prefix `/api/customer/...` thay vì `/api/v1/...` như phần còn lại của hệ thống.
- Nếu cần thống nhất, nên cân nhắc đổi thành `/api/v1/customer/parking-orders/active`.

## File liên quan

- Controller: `src/main/java/com/java/parking/controller/CustomerParkingOrderController.java`
- Service: `src/main/java/com/java/parking/service/CustomerParkingOrderService.java`
- DTO Response: `ActiveParkingOrderResponse.java`
