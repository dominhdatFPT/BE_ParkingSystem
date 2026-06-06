# Tóm gọn luồng 3 Flow - Smart Parking

## Flow 1 — Xem thông tin xe đang gửi
`GET /api/parking/me/vehicles`

```
Client → Controller → VehicleInfoService → ParkingOrderRepository
       ← JSON 200  ←  List<VehicleInfoResponse> ←  List<ParkingOrder>
```

**Bước chạy:**
1. Client gửi request kèm JWT token
2. Controller đọc `userId` từ SecurityContext
3. Service gọi query lấy tất cả đơn `ACTIVE` / `CHECKED_IN` của user
4. Map từng đơn → biển số, hãng, màu, loại xe, trạng thái, tên bãi, tầng, giờ vào
5. Trả về list (rỗng nếu chưa gửi xe)

---

## Flow 2 — Theo dõi vị trí và thời gian gửi xe
`GET /api/parking/me/locations`

```
Client → Controller → ParkingLocationService → ParkingOrderRepository
       ← JSON 200  ←  List<ParkingLocationResponse> ← List<ParkingOrder>
```

**Bước chạy:**
1. Client gửi request kèm JWT token
2. Controller đọc `userId` từ SecurityContext
3. Service gọi query lấy tất cả đơn active của user
4. Tính `durationMinutes` = thời gian hiện tại - `entryTime`
5. Map → tên bãi, tên tầng, số tầng, giờ vào, số phút đã gửi
6. Trả về list

---

## Flow 3 — Xem phí gửi xe tạm tính
`GET /api/parking/me/estimated-fees`

```
Client → Controller → EstimatedFeeService → ParkingOrderRepository
                                          → PricingRuleRepository
       ← JSON 200  ← List<EstimatedFeeResponse>
```

**Bước chạy:**
1. Client gửi request kèm JWT token
2. Controller đọc `userId` từ SecurityContext
3. Service lấy tất cả đơn active của user
4. Với mỗi đơn:
   - Tính `durationMinutes`
   - Xác định `dayType` (WEEKDAY / WEEKEND)
   - Tìm pricing rule phù hợp theo: bãi + loại xe + khung giờ + loại ngày + khoảng phút
   - `phí = đơn giá × số phút` (= 0 nếu không có rule phù hợp)
5. Trả về list phí ước tính
