# Parking API

> Quản lý cấu trúc bãi đỗ xe: slot, tầng, khu vực, bản đồ bãi xe.

## Endpoints

### Parking Area Summary (Public)

| Method | Endpoint | Mô tả | Auth | Query Params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/parking-area-summary/options` | Lấy danh sách options lọc | Public | - |
| `GET` | `/api/v1/parking-area-summary` | Tổng hợp khu vực đỗ xe | Public | `buildingCode`, `floorNumber` |

### Parking Slots

| Method | Endpoint | Mô tả | Auth | Request DTO |
|--------|----------|-------|------|-------------|
| `GET` | `/api/v1/parking-slots` | Lấy tất cả slots | Authenticated | - |
| `GET` | `/api/v1/parking-slots/{id}` | Lấy slot theo ID | Authenticated | - |
| `POST` | `/api/v1/parking-slots` | Tạo slot mới | Authenticated | `ParkingSlotRequest` |
| `PUT` | `/api/v1/parking-slots/{id}` | Cập nhật slot | Authenticated | `ParkingSlotRequest` |
| `DELETE` | `/api/v1/parking-slots/{id}` | Xóa slot | Authenticated | - |

### Parking Map / Structure

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| `GET` | `/api/v1/parking-map/facility/{parkingId}` | Lấy bản đồ bãi xe theo facility | Authenticated |

## Chi tiết

### `GET /api/v1/parking-area-summary`

**Query params:**
- `buildingCode` (optional)
- `floorNumber` (optional)

Trả về danh sách `ParkingAreaSummaryResponse`.

### `GET /api/v1/parking-map/facility/{parkingId}`

Trả về cấu trúc tầng + slot của một parking facility.

## File liên quan

- Controllers:
  - `src/main/java/com/java/parking/controller/ParkingAreaSummaryController.java`
  - `src/main/java/com/java/parking/controller/ParkingSlotController.java`
  - `src/main/java/com/java/parking/controller/ParkingStructureController.java`
- Services:
  - `src/main/java/com/java/parking/service/ParkingAreaSummaryService.java`
  - `src/main/java/com/java/parking/service/ParkingSlotService.java`
  - `src/main/java/com/java/parking/service/ParkingStructureService.java`
- DTO Response:
  - `ParkingAreaSummaryResponse.java`
  - `ParkingAreaOptionsResponse.java`
  - `ParkingSlotResponse.java`
  - `ParkingFloorResponse.java`
