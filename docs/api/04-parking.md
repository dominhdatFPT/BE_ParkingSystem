# Parking API

## Parking Area Summary

| Method | Endpoint | Quyền | Mục đích |
|--------|----------|-------|----------|
| `GET` | `/api/v1/parking-area-summary/options` | Public | Danh sách tùy chọn lọc |
| `GET` | `/api/v1/parking-area-summary` | Public | Tổng hợp sức chứa và slot trống |

Query params của API tổng hợp:

- `buildingCode` (không bắt buộc)
- `floorNumber` (không bắt buộc)

## Parking Slots

| Method | Endpoint | Quyền |
|--------|----------|-------|
| `GET` | `/api/v1/parking-slots` | `ADMIN`, `STAFF` |
| `GET` | `/api/v1/parking-slots/{id}` | `ADMIN`, `STAFF` |
| `POST` | `/api/v1/parking-slots` | `ADMIN`, `STAFF` |
| `PUT` | `/api/v1/parking-slots/{id}` | `ADMIN`, `STAFF` |
| `DELETE` | `/api/v1/parking-slots/{id}` | `ADMIN`, `STAFF` |

API bản đồ bãi xe cũ đã được loại khỏi source.
