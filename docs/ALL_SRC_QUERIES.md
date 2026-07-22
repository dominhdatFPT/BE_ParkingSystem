# Tổng hợp toàn bộ query trong src/main/java

> Báo cáo được sinh trực tiếp từ source bởi `scripts/export-src-queries.ps1`. Mỗi mục ghi loại query, mục đích, vị trí, bảng/entity và cột/thuộc tính liên quan.

- Tổng số mục: 177
- Phạm vi quét: `D:\LEARN_FPT\SWP\src\BE\src\main\java`
- Đây là tài liệu kiểm kê source, không phải tập lệnh migration hoặc script chạy trực tiếp trên database.
- Với JPQL/Spring Data/JpaRepository, SQL thực tế do Hibernate sinh từ entity mapping.

## Query khai báo bằng @Query

### `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java:21`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Xóa token thiết bị theo điều kiện trong mệnh đề `WHERE`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java`, bắt đầu tại dòng **21**.

**Bảng/entity liên quan:** `device_tokens`

**Cột/thuộc tính liên quan:** `device_tokens.token`

```jpql
DELETE FROM DeviceToken d WHERE d.token = :token
```

### `src/main/java/com/swp/parking/repository/FeePackageRepository.java:16`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc các bản ghi gói phí đang hoạt động.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeePackageRepository.java`, bắt đầu tại dòng **16**.

**Bảng/entity liên quan:** `fee_package`

**Cột/thuộc tính liên quan:** `fee_package.is_active`, `fee_package.vehicle_type_id`

```jpql
SELECT fp FROM FeePackage fp WHERE fp.vehicleType.id = :vehicleTypeId AND fp.isActive = true
```

### `src/main/java/com/swp/parking/repository/FeePackageRepository.java:19`

**Loại query:** Native SQL PostgreSQL khai báo bằng `@Query`.

**Mục đích:** Lấy gói phí đang hoạt động và mức giá hiệu lực gần nhất trong một lần truy vấn.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeePackageRepository.java`, bắt đầu tại dòng **19**.

**Bảng/entity liên quan:** `fee_package`, `fee_package_price_history`, `LATERAL`, `vehicle_types`

**Cột/thuộc tính liên quan:** `effective_from`, `fee_package.benefits`, `fee_package.duration_months`, `fee_package.fee_package_id`, `fee_package.is_active`, `fee_package.is_best_value`, `fee_package.is_popular`, `fee_package.name`, `fee_package.vehicle_type_id`, `fee_package_price_history.discount_percent`, `fee_package_price_history.effective_from`, `fee_package_price_history.fee_package_id`, `fee_package_price_history.original_price`, `fee_package_price_history.price`, `vehicle_types.type_name`, `vehicle_types.vehicle_type_id`

```sql
SELECT fp.fee_package_id AS id,
                   vt.vehicle_type_id AS "vehicleTypeId",
                   vt.type_name AS "vehicleTypeName",
                   fp.name AS name,
                   fp.duration_months AS "durationMonths",
                   fp.benefits AS benefits,
                   fp.is_popular AS "isPopular",
                   fp.is_best_value AS "isBestValue",
                   current_price.price AS "currentPrice",
                   current_price.original_price AS "originalPrice",
                   current_price.discount_percent AS "discountPercent"
              FROM fee_package fp
              JOIN vehicle_types vt ON vt.vehicle_type_id = fp.vehicle_type_id
              LEFT JOIN LATERAL (
                    SELECT ph.price,
                           ph.original_price,
                           ph.discount_percent
                      FROM fee_package_price_history ph
                     WHERE ph.fee_package_id = fp.fee_package_id
                       AND ph.effective_from <= :at
                     ORDER BY ph.effective_from DESC
                     LIMIT 1
              ) current_price ON true
             WHERE fp.is_active = true
               AND (:vehicleTypeId IS NULL OR fp.vehicle_type_id = :vehicleTypeId)
             ORDER BY fp.duration_months, fp.fee_package_id
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionInvoiceRepository.java:14`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc hóa đơn gói phí thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionInvoiceRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `fee_package`, `fee_subscription`, `fee_subscription_invoice`, `vehicles`

**Cột/thuộc tính liên quan:** `fee_subscription.fee_package_id`, `fee_subscription.vehicle_id`, `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`, `vehicles.customer_id`

```jpql
SELECT fsi FROM FeeSubscriptionInvoice fsi
            JOIN FETCH fsi.feeSubscription fs
            JOIN FETCH fs.vehicle v
            JOIN FETCH fs.feePackage pkg
            WHERE v.customer.user.id = :userId
            ORDER BY fsi.createdAt DESC
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java:14`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc đăng ký gói phí thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `customers`, `fee_subscription`, `vehicles`

**Cột/thuộc tính liên quan:** `customers.user_id`, `fee_subscription.created_at`, `fee_subscription.vehicle_id`, `vehicles.customer_id`

```jpql
SELECT fs FROM FeeSubscription fs JOIN FETCH fs.vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId ORDER BY fs.createdAt DESC
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java:23`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc đăng ký gói phí thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java`, bắt đầu tại dòng **23**.

**Bảng/entity liên quan:** `fee_package`, `fee_subscription`, `vehicles`

**Cột/thuộc tính liên quan:** `fee_subscription.created_at`, `fee_subscription.fee_package_id`, `fee_subscription.price_history_id`, `fee_subscription.vehicle_id`, `vehicles.customer_id`

```jpql
SELECT fs FROM FeeSubscription fs
            JOIN FETCH fs.vehicle v
            JOIN FETCH fs.feePackage
            JOIN FETCH fs.priceHistory
            WHERE v.customer.user.id = :userId
            ORDER BY fs.createdAt DESC
```

### `src/main/java/com/swp/parking/repository/NotificationRepository.java:17`

**Loại query:** Native SQL PostgreSQL khai báo bằng `@Query`.

**Mục đích:** Đọc thông báo theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/NotificationRepository.java`, bắt đầu tại dòng **17**.

**Bảng/entity liên quan:** `Notification`

**Cột/thuộc tính liên quan:** `Notification.isActive`, `Notification.publishedAt`, `Notification.recipientTarget`, `Notification.recipientUserId`, `Notification.status`

```sql
SELECT n FROM Notification n
            WHERE n.isActive = true
              AND n.status = :status
              AND n.recipientUserId IS NULL
              AND n.recipientTarget <> com.swp.parking.model.enums.NotificationRecipientTarget.SPECIFIC_USER
            ORDER BY n.publishedAt DESC
```

### `src/main/java/com/swp/parking/repository/NotificationRepository.java:30`

**Loại query:** Native SQL PostgreSQL khai báo bằng `@Query`.

**Mục đích:** Đọc thông báo theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/NotificationRepository.java`, bắt đầu tại dòng **30**.

**Bảng/entity liên quan:** `Notification`

**Cột/thuộc tính liên quan:** `Notification.isActive`, `Notification.publishedAt`, `Notification.recipientTarget`, `Notification.recipientUserId`, `Notification.status`

```sql
SELECT n FROM Notification n
            WHERE n.isActive = true AND n.status = :status
              AND (n.recipientUserId = :userId
                   OR (n.recipientUserId IS NULL AND n.recipientTarget <> com.swp.parking.model.enums.NotificationRecipientTarget.SPECIFIC_USER))
            ORDER BY n.publishedAt DESC
```

### `src/main/java/com/swp/parking/repository/ParkingOrderRepository.java:12`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đếm hoặc tổng hợp số liệu phiên/đơn gửi xe để phục vụ kiểm tra và dashboard.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingOrderRepository.java`, bắt đầu tại dòng **12**.

**Bảng/entity liên quan:** `parking_orders`, `vehicle_types`, `vehicles`

**Cột/thuộc tính liên quan:** `parking_orders.floor_id`, `parking_orders.parking_id`, `parking_orders.parking_status`, `parking_orders.vehicle_id`, `vehicle_types.vehicle_type_id`, `vehicles.vehicle_type_id`

```jpql
SELECT COUNT(po)
        FROM ParkingOrder po
        JOIN po.vehicle v
        JOIN v.vehicleType vt
        WHERE po.parkingFacility.id = :parkingId
          AND po.parkingFloor.id = :floorId
          AND vt.id = :vehicleTypeId
          AND po.parkingStatus = 'ACTIVE'
```

### `src/main/java/com/swp/parking/repository/UserRepository.java:18`

**Loại query:** Native SQL PostgreSQL khai báo bằng `@Query`.

**Mục đích:** Đọc người dùng thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/UserRepository.java`, bắt đầu tại dòng **18**.

**Bảng/entity liên quan:** `employees`

**Cột/thuộc tính liên quan:** `employees.role`, `employees.status`, `employees.user_id`

```sql
select e.role from employees e where e.user_id = :userId and e.status = 'ACTIVE' limit 1
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:21`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc đăng ký phương tiện theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **21**.

**Bảng/entity liên quan:** `fee_package`, `users`, `vehicle_registrations`, `vehicle_types`

**Cột/thuộc tính liên quan:** `fee_package.fee_package_id`, `fee_package.name`, `users.full_name`, `users.user_id`, `vehicle_registrations.brand`, `vehicle_registrations.color`, `vehicle_registrations.contact_phone`, `vehicle_registrations.created_at`, `vehicle_registrations.ekyc_cccd_id`, `vehicle_registrations.ekyc_confidence_score`, `vehicle_registrations.ekyc_full_name`, `vehicle_registrations.ekyc_is_fake`, `vehicle_registrations.ekyc_is_valid`, `vehicle_registrations.ekyc_license_class`, `vehicle_registrations.ekyc_license_number`, `vehicle_registrations.license_plate`, `vehicle_registrations.registration_id`, `vehicle_registrations.registration_source`, `vehicle_registrations.reject_reason`, `vehicle_registrations.requested_fee_package_id`, `vehicle_registrations.reviewed_at`, `vehicle_registrations.status`, `vehicle_registrations.user_id`, `vehicle_registrations.vehicle_type_id`, `vehicle_types.type_name`, `vehicle_types.vehicle_type_id`

```jpql
SELECT r.id AS registrationId,
                   u.id AS userId,
                   u.fullName AS userFullName,
                   vt.id AS vehicleTypeId,
                   vt.typeName AS vehicleTypeName,
                   r.licensePlate AS licensePlate,
                   r.contactPhone AS contactPhone,
                   fp.id AS requestedFeePackageId,
                   fp.name AS requestedFeePackageName,
                   r.registrationSource AS registrationSource,
                   r.brand AS brand,
                   r.color AS color,
                   r.status AS status,
                   r.rejectReason AS rejectReason,
                   r.ekycFullName AS ekycFullName,
                   r.ekycCccdId AS ekycCccdId,
                   r.ekycLicenseNumber AS ekycLicenseNumber,
                   r.ekycLicenseClass AS ekycLicenseClass,
                   r.ekycIsValid AS ekycIsValid,
                   r.ekycIsFake AS ekycIsFake,
                   r.ekycConfidenceScore AS ekycConfidenceScore,
                   r.createdAt AS createdAt,
                   r.reviewedAt AS reviewedAt
              FROM VehicleRegistration r
              JOIN r.user u
              JOIN r.vehicleType vt
              LEFT JOIN r.requestedFeePackage fp
             WHERE (:status IS NULL OR r.status = :status)
             ORDER BY r.createdAt DESC
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:54`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc đăng ký phương tiện theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **54**.

**Bảng/entity liên quan:** `fee_package`, `users`, `vehicle_registrations`, `vehicle_types`

**Cột/thuộc tính liên quan:** `fee_package.fee_package_id`, `fee_package.name`, `users.full_name`, `users.user_id`, `vehicle_registrations.brand`, `vehicle_registrations.color`, `vehicle_registrations.contact_phone`, `vehicle_registrations.created_at`, `vehicle_registrations.ekyc_cccd_id`, `vehicle_registrations.ekyc_confidence_score`, `vehicle_registrations.ekyc_full_name`, `vehicle_registrations.ekyc_is_fake`, `vehicle_registrations.ekyc_is_valid`, `vehicle_registrations.ekyc_license_class`, `vehicle_registrations.ekyc_license_number`, `vehicle_registrations.license_plate`, `vehicle_registrations.registration_id`, `vehicle_registrations.registration_source`, `vehicle_registrations.reject_reason`, `vehicle_registrations.requested_fee_package_id`, `vehicle_registrations.reviewed_at`, `vehicle_registrations.status`, `vehicle_registrations.user_id`, `vehicle_registrations.vehicle_type_id`, `vehicle_types.type_name`, `vehicle_types.vehicle_type_id`

```jpql
SELECT r.id AS registrationId,
                   u.id AS userId,
                   u.fullName AS userFullName,
                   vt.id AS vehicleTypeId,
                   vt.typeName AS vehicleTypeName,
                   r.licensePlate AS licensePlate,
                   r.contactPhone AS contactPhone,
                   fp.id AS requestedFeePackageId,
                   fp.name AS requestedFeePackageName,
                   r.registrationSource AS registrationSource,
                   r.brand AS brand,
                   r.color AS color,
                   r.status AS status,
                   r.rejectReason AS rejectReason,
                   r.ekycFullName AS ekycFullName,
                   r.ekycCccdId AS ekycCccdId,
                   r.ekycLicenseNumber AS ekycLicenseNumber,
                   r.ekycLicenseClass AS ekycLicenseClass,
                   r.ekycIsValid AS ekycIsValid,
                   r.ekycIsFake AS ekycIsFake,
                   r.ekycConfidenceScore AS ekycConfidenceScore,
                   r.createdAt AS createdAt,
                   r.reviewedAt AS reviewedAt
              FROM VehicleRegistration r
              JOIN r.user u
              JOIN r.vehicleType vt
              LEFT JOIN r.requestedFeePackage fp
             WHERE u.id = :userId
             ORDER BY r.createdAt DESC
```

### `src/main/java/com/swp/parking/repository/VehicleRepository.java:13`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc phương tiện thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRepository.java`, bắt đầu tại dòng **13**.

**Bảng/entity liên quan:** `customers`, `vehicles`

**Cột/thuộc tính liên quan:** `customers.user_id`, `vehicles.created_at`, `vehicles.customer_id`

```jpql
SELECT v FROM Vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId ORDER BY v.createdAt DESC
```

### `src/main/java/com/swp/parking/repository/VehicleRepository.java:16`

**Loại query:** JPQL/HQL khai báo bằng `@Query`; Hibernate chuyển thành SQL PostgreSQL khi chạy.

**Mục đích:** Đọc phương tiện thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRepository.java`, bắt đầu tại dòng **16**.

**Bảng/entity liên quan:** `customers`, `vehicles`

**Cột/thuộc tính liên quan:** `customers.user_id`, `vehicles.created_at`, `vehicles.customer_id`, `vehicles.vehicle_type_id`

```jpql
SELECT v FROM Vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId AND v.vehicleType.id = :vehicleTypeId ORDER BY v.createdAt DESC
```

## SQL viết trực tiếp trong JdbcTemplate

### `src/main/java/com/swp/parking/config/SupportSchemaInitializer.java:18`

**Loại query:** DDL PostgreSQL dạng `ALTER TABLE`; thay đổi schema.

**Mục đích:** Thay đổi cấu trúc schema hỗ trợ để bổ sung hoặc điều chỉnh cột phục vụ backend.

**Vị trí trong source:** `src/main/java/com/swp/parking/config/SupportSchemaInitializer.java`, bắt đầu tại dòng **18**.

**Bảng/entity liên quan:** `parking_incidents`

**Cột/thuộc tính liên quan:** `reply_title`

```sql
ALTER TABLE parking_incidents
                ADD COLUMN IF NOT EXISTS reply_title VARCHAR(200)
```

### `src/main/java/com/swp/parking/config/SupportSchemaInitializer.java:22`

**Loại query:** DDL PostgreSQL dạng `ALTER TABLE`; thay đổi schema.

**Mục đích:** Thay đổi cấu trúc schema hỗ trợ để bổ sung hoặc điều chỉnh cột phục vụ backend.

**Vị trí trong source:** `src/main/java/com/swp/parking/config/SupportSchemaInitializer.java`, bắt đầu tại dòng **22**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.recipient_user_id`, `recipient_user_id`

```sql
ALTER TABLE notifications
                ADD COLUMN IF NOT EXISTS recipient_user_id BIGINT
```

### `src/main/java/com/swp/parking/config/SupportSchemaInitializer.java:26`

**Loại query:** DDL PostgreSQL dạng `CREATE`; tạo bảng hoặc index.

**Mục đích:** Tạo cấu trúc database cho schema hỗ trợ nếu chưa tồn tại.

**Vị trí trong source:** `src/main/java/com/swp/parking/config/SupportSchemaInitializer.java`, bắt đầu tại dòng **26**.

**Bảng/entity liên quan:** Không xác định tự động; xem entity/method tại vị trí source phía trên.

**Cột/thuộc tính liên quan:** `recipient_user_id`

```sql
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id
                ON notifications(recipient_user_id)
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:156`

**Loại query:** Native SQL PostgreSQL dạng `UPDATE` qua `JdbcTemplate`.

**Mục đích:** Cập nhật xe vào bãi theo điều kiện trong mệnh đề `WHERE`.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **156**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** `current_order_id`, `visitor_card_id`

```sql
UPDATE visitor_cards
                SET status = 'IN_USE',
                    current_order_id = ?,
                    updated_at = now()
                WHERE visitor_card_id = ?
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:213`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc xe vào bãi thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **213**.

**Bảng/entity liên quan:** `customers`, `fee_package`, `fee_subscription`, `users`, `vehicle_types`, `vehicles`

**Cột/thuộc tính liên quan:** `customers.customer_id`, `customers.user_id`, `fee_package.fee_package_id`, `fee_package.name`, `fee_package.vehicle_type_id`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.status`, `fee_subscription.vehicle_id`, `users.full_name`, `users.user_id`, `vehicle_types.type_name`, `vehicle_types.vehicle_type_id`, `vehicles.brand`, `vehicles.color`, `vehicles.customer_id`, `vehicles.license_plate`, `vehicles.vehicle_id`, `vehicles.vehicle_type_id`

```sql
SELECT v.vehicle_id,
                           v.license_plate,
                           v.brand,
                           v.color,
                           u.full_name AS customer_name,
                           vt.vehicle_type_id,
                           vt.type_name AS vehicle_type,
                           fs.fee_subscription_id,
                           fp.name AS package_name,
                           fs.end_date
                    FROM vehicles v
                    JOIN customers c ON c.customer_id = v.customer_id
                    JOIN users u ON u.user_id = c.user_id
                    JOIN fee_subscription fs ON fs.vehicle_id = v.vehicle_id
                    JOIN fee_package fp ON fp.fee_package_id = fs.fee_package_id
                    LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                    WHERE regexp_replace(upper(v.license_plate), '[^A-Z0-9]', '', 'g') = ?
                      AND upper(fs.status) IN (?, ?, ?, ?)
                      AND (fs.end_date IS NULL OR fs.end_date >= now())
                    ORDER BY fs.end_date DESC NULLS FIRST
                    LIMIT 1
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:263`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc xe vào bãi theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **263**.

**Bảng/entity liên quan:** `vehicle_types`

**Cột/thuộc tính liên quan:** `vehicle_types.type_code`, `vehicle_types.type_name`, `vehicle_types.vehicle_type_id`

```sql
SELECT vehicle_type_id
                FROM vehicle_types
                WHERE upper(COALESCE(type_code, '')) = ?
                   OR (? = 'MOTORBIKE' AND upper(type_name) LIKE '%MOTOR%')
                   OR (? = 'CAR' AND upper(type_name) NOT LIKE '%MOTOR%')
                ORDER BY CASE WHEN upper(COALESCE(type_code, '')) = ? THEN 0 ELSE 1 END
                LIMIT 1
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:276`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đếm hoặc tổng hợp số liệu xe vào bãi để phục vụ kiểm tra và dashboard.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **276**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `parking_orders.license_plate`, `parking_orders.parking_status`

```sql
SELECT count(*)
                FROM parking_orders
                WHERE regexp_replace(upper(license_plate), '[^A-Z0-9]', '', 'g') = ?
                  AND parking_status = 'ACTIVE'
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:287`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc xe vào bãi theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **287**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** `card_code`, `display_number`, `visitor_card_id`

```sql
SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE status = 'AVAILABLE'
                    ORDER BY display_number
                    LIMIT 1
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:423`

**Loại query:** DDL PostgreSQL dạng `CREATE`; tạo bảng hoặc index.

**Mục đích:** Tạo cấu trúc database cho xe vào bãi nếu chưa tồn tại.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **423**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `calculated_fee`, `checked_in_by`, `order_code`, `order_id`, `parking_orders.calculated_fee`, `parking_orders.card_id`, `parking_orders.checked_in_by`, `parking_orders.created_at`, `parking_orders.entry_gate_id`, `parking_orders.entry_time`, `parking_orders.exit_time`, `parking_orders.floor_id`, `parking_orders.license_plate`, `parking_orders.notes`, `parking_orders.order_code`, `parking_orders.order_id`, `parking_orders.parking_id`, `parking_orders.parking_status`, `parking_orders.updated_at`, `parking_orders.vehicle_id`

```sql
CREATE TABLE IF NOT EXISTS parking_orders (
                        order_id bigserial PRIMARY KEY,
                        order_code varchar(64) UNIQUE,
                        parking_id bigint,
                        floor_id bigint,
                        vehicle_id bigint,
                        license_plate varchar(50),
                        entry_gate_id bigint,
                        card_id bigint,
                        entry_time timestamp,
                        exit_time timestamp,
                        calculated_fee numeric(19, 2),
                        parking_status varchar(30),
                        checked_in_by bigint,
                        notes text,
                        created_at timestamp NOT NULL DEFAULT now(),
                        updated_at timestamp NOT NULL DEFAULT now()
                    )
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:443`

**Loại query:** DDL PostgreSQL dạng `CREATE`; tạo bảng hoặc index.

**Mục đích:** Tạo cấu trúc database cho xe vào bãi nếu chưa tồn tại.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **443**.

**Bảng/entity liên quan:** Không xác định tự động; xem entity/method tại vị trí source phía trên.

**Cột/thuộc tính liên quan:** Không ghi cứng trong source; Hibernate quyết định theo entity mapping hoặc câu query động.

```sql
CREATE INDEX IF NOT EXISTS idx_parking_orders_plate_status
                    ON parking_orders (license_plate, parking_status)
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:447`

**Loại query:** DDL PostgreSQL dạng `CREATE`; tạo bảng hoặc index.

**Mục đích:** Tạo cấu trúc database cho xe vào bãi nếu chưa tồn tại.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **447**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** `card_code`, `current_order_id`, `display_number`, `visitor_card_id`

```sql
CREATE TABLE IF NOT EXISTS visitor_cards (
                        visitor_card_id bigserial PRIMARY KEY,
                        card_code varchar(20) NOT NULL UNIQUE,
                        display_number integer NOT NULL UNIQUE,
                        status varchar(30) NOT NULL DEFAULT 'AVAILABLE',
                        current_order_id bigint,
                        created_at timestamp NOT NULL DEFAULT now(),
                        updated_at timestamp NOT NULL DEFAULT now()
                    )
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:458`

**Loại query:** Native SQL PostgreSQL dạng `INSERT` qua `JdbcTemplate`.

**Mục đích:** Thêm dữ liệu xe vào bãi vào database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **458**.

**Bảng/entity liên quan:** `generate_series`, `visitor_cards`

**Cột/thuộc tính liên quan:** `card_code`, `display_number`

```sql
INSERT INTO visitor_cards (card_code, display_number, status, created_at, updated_at)
                    SELECT concat('VIS', lpad(n::text, 3, '0')), n, 'AVAILABLE', now(), now()
                    FROM generate_series(1, 100) AS n
                    ON CONFLICT (card_code) DO NOTHING
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:464`

**Loại query:** DDL PostgreSQL dạng `CREATE`; tạo bảng hoặc index.

**Mục đích:** Tạo cấu trúc database cho xe vào bãi nếu chưa tồn tại.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **464**.

**Bảng/entity liên quan:** Không xác định tự động; xem entity/method tại vị trí source phía trên.

**Cột/thuộc tính liên quan:** `display_number`

```sql
CREATE INDEX IF NOT EXISTS idx_visitor_cards_status_number
                    ON visitor_cards (status, display_number)
```

### `src/main/java/com/swp/parking/service/ParkingExitService.java:67`

**Loại query:** Native SQL PostgreSQL dạng `UPDATE` qua `JdbcTemplate`.

**Mục đích:** Cập nhật xe ra bãi theo điều kiện trong mệnh đề `WHERE`.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingExitService.java`, bắt đầu tại dòng **67**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `calculated_fee`, `checked_out_by`, `checkout_confirmed_at`, `fee_breakdown`, `fee_rate_id`, `order_id`, `parking_orders.calculated_fee`, `parking_orders.checked_out_by`, `parking_orders.checkout_confirmed_at`, `parking_orders.exit_time`, `parking_orders.fee_breakdown`, `parking_orders.fee_rate_id`, `parking_orders.order_id`, `parking_orders.parking_status`, `parking_orders.payment_method`, `parking_orders.payment_status`, `parking_orders.updated_at`, `payment_method`, `payment_status`

```sql
UPDATE parking_orders
                   SET exit_time = ?,
                       calculated_fee = ?,
                       parking_status = 'COMPLETED',
                       checked_out_by = ?,
                       checkout_confirmed_at = ?,
                       payment_status = ?,
                       payment_method = ?,
                       fee_rate_id = ?,
                       fee_breakdown = ?::jsonb,
                       updated_at = now()
                 WHERE order_id = ?
                   AND parking_status = 'ACTIVE'
```

### `src/main/java/com/swp/parking/service/ParkingExitService.java:98`

**Loại query:** Native SQL PostgreSQL dạng `INSERT` qua `JdbcTemplate`.

**Mục đích:** Thêm dữ liệu xe ra bãi vào database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingExitService.java`, bắt đầu tại dòng **98**.

**Bảng/entity liên quan:** `parking_order_payments`

**Cột/thuộc tính liên quan:** `order_id`, `payment_method`, `payment_status`

```sql
INSERT INTO parking_order_payments (
                        order_id, amount, payment_method, payment_status,
                        received_by, paid_at, created_at, updated_at
                    )
                    VALUES (?, ?, ?, 'PAID', ?, ?, now(), now())
```

### `src/main/java/com/swp/parking/service/ParkingExitService.java:106`

**Loại query:** Native SQL PostgreSQL dạng `UPDATE` qua `JdbcTemplate`.

**Mục đích:** Cập nhật xe ra bãi theo điều kiện trong mệnh đề `WHERE`.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingExitService.java`, bắt đầu tại dòng **106**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** `current_order_id`, `visitor_card_id`

```sql
UPDATE visitor_cards
                       SET status = 'AVAILABLE', current_order_id = NULL, updated_at = now()
                     WHERE current_order_id = ? OR visitor_card_id = ?
```

### `src/main/java/com/swp/parking/service/ParkingExitService.java:284`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc các bản ghi xe ra bãi đang hoạt động.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingExitService.java`, bắt đầu tại dòng **284**.

**Bảng/entity liên quan:** `visitor_fee_rates`

**Cột/thuộc tính liên quan:** `daily_cap`, `effective_from`, `effective_to`, `fee_rate_id`, `first_block_fee`, `first_block_minutes`, `next_block_fee`, `next_block_minutes`, `overnight_fee`

```sql
SELECT fee_rate_id, first_block_minutes, first_block_fee,
                       next_block_minutes, next_block_fee, daily_cap, overnight_fee
                  FROM visitor_fee_rates
                 WHERE vehicle_type_id = ?
                   AND is_active = true
                   AND (parking_id = ? OR parking_id IS NULL)
                   AND effective_from <= ?
                   AND (effective_to IS NULL OR effective_to > ?)
                 ORDER BY CASE WHEN parking_id = ? THEN 0 ELSE 1 END, effective_from DESC
                 LIMIT 1
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:40`

**Loại query:** Native SQL PostgreSQL dạng `INSERT` qua `JdbcTemplate`.

**Mục đích:** Thêm dữ liệu cấu hình, sự cố và audit hệ thống vào database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **40**.

**Bảng/entity liên quan:** `system_configuration`

**Cột/thuộc tính liên quan:** `config_data`, `config_id`, `updated_by`

```sql
INSERT INTO system_configuration(config_id, config_data, updated_by, updated_at)
                    VALUES (1, ?::jsonb, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (config_id) DO UPDATE SET config_data = EXCLUDED.config_data,
                        updated_by = EXCLUDED.updated_by, updated_at = CURRENT_TIMESTAMP
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:65`

**Loại query:** Native SQL PostgreSQL dạng `INSERT` qua `JdbcTemplate`.

**Mục đích:** Thêm dữ liệu cấu hình, sự cố và audit hệ thống vào database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **65**.

**Bảng/entity liên quan:** `parking_incidents`

**Cột/thuộc tính liên quan:** `created_by`, `description`, `incident_id`, `incident_type`, `severity`

```sql
INSERT INTO parking_incidents(incident_type, severity, description, status, created_by)
                VALUES (?, 'INFO', ?, 'OPEN', ?)
                RETURNING incident_id
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:76`

**Loại query:** Native SQL PostgreSQL dạng `UPDATE` qua `JdbcTemplate`.

**Mục đích:** Cập nhật cấu hình, sự cố và audit hệ thống theo điều kiện trong mệnh đề `WHERE`.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **76**.

**Bảng/entity liên quan:** `parking_incidents`

**Cột/thuộc tính liên quan:** `incident_id`, `reply_title`, `resolution`, `resolved_at`, `resolved_by`

```sql
UPDATE parking_incidents
                SET reply_title = ?, resolution = ?, status = ?, resolved_by = ?, resolved_at = CURRENT_TIMESTAMP
                WHERE incident_id = ?
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:94`

**Loại query:** Native SQL PostgreSQL dạng `INSERT` qua `JdbcTemplate`.

**Mục đích:** Thêm dữ liệu cấu hình, sự cố và audit hệ thống vào database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **94**.

**Bảng/entity liên quan:** `parking_incidents`

**Cột/thuộc tính liên quan:** `card_code`, `created_by`, `description`, `incident_id`, `incident_type`, `resolution`, `severity`

```sql
INSERT INTO parking_incidents(incident_type, severity, license_plate, card_code,
                    description, resolution, created_by)
                VALUES (?, ?, NULLIF(?, ''), NULLIF(?, ''), ?, NULLIF(?, ''), ?)
                RETURNING incident_id
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:108`

**Loại query:** Native SQL PostgreSQL dạng `UPDATE` qua `JdbcTemplate`.

**Mục đích:** Cập nhật cấu hình, sự cố và audit hệ thống theo điều kiện trong mệnh đề `WHERE`.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **108**.

**Bảng/entity liên quan:** `parking_incidents`

**Cột/thuộc tính liên quan:** `incident_id`, `resolution`, `resolved_at`, `resolved_by`

```sql
UPDATE parking_incidents SET status = 'CLOSED', resolution = ?, resolved_by = ?,
                    resolved_at = CURRENT_TIMESTAMP WHERE incident_id = ? AND status = 'OPEN'
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:119`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc cấu hình, sự cố và audit hệ thống thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **119**.

**Bảng/entity liên quan:** `audit_logs`, `users`

**Cột/thuộc tính liên quan:** `action`, `audit_id`, `audit_logs.action`, `audit_logs.audit_id`, `audit_logs.created_at`, `audit_logs.message`, `audit_logs.severity`, `audit_logs.status`, `audit_logs.user_id`, `message`, `severity`, `users.created_at`, `users.email`, `users.full_name`, `users.user_id`

```sql
SELECT a.audit_id AS id, a.action, a.status, a.severity, a.message,
                       a.created_at, u.full_name AS user_name, u.email
                FROM audit_logs a LEFT JOIN users u ON u.user_id = a.user_id
                ORDER BY a.created_at DESC LIMIT 500
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:128`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc cấu hình, sự cố và audit hệ thống theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **128**.

**Bảng/entity liên quan:** `parking_incidents`

**Cột/thuộc tính liên quan:** `card_code`, `description`, `incident_id`, `incident_type`, `resolution`, `severity`

```sql
SELECT incident_id AS id, incident_type AS type, severity, license_plate AS plate,
                       card_code, description, resolution, status, created_at
                FROM parking_incidents WHERE incident_id = ?
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:159`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc cấu hình, sự cố và audit hệ thống thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **159**.

**Bảng/entity liên quan:** `parking_incidents`, `users`

**Cột/thuộc tính liên quan:** `created_by`, `description`, `incident_id`, `incident_type`, `parking_incidents.created_at`, `parking_incidents.created_by`, `parking_incidents.description`, `parking_incidents.incident_id`, `parking_incidents.incident_type`, `parking_incidents.reply_title`, `parking_incidents.resolution`, `parking_incidents.resolved_at`, `parking_incidents.resolved_by`, `parking_incidents.status`, `reply_title`, `resolution`, `resolved_at`, `resolved_by`, `users.created_at`, `users.email`, `users.full_name`, `users.user_id`

```sql
SELECT i.incident_id AS id,
                       u.full_name AS "userName",
                       u.email AS "userEmail",
                       i.incident_type AS service,
                       i.description AS content,
                       i.status,
                       i.created_at AS "createdAt",
                       i.reply_title AS "replyTitle",
                       i.resolution AS "replyMessage",
                       resolver.full_name AS "repliedBy",
                       i.resolved_at AS "repliedAt"
                FROM parking_incidents i
                LEFT JOIN users u ON u.user_id = i.created_by
                LEFT JOIN users resolver ON resolver.user_id = i.resolved_by
                WHERE i.incident_id = ?
```

## SQL dùng PreparedStatement

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:353`

**Loại query:** Native SQL PostgreSQL dùng `PreparedStatement` và tham số `?`.

**Mục đích:** Thêm dữ liệu xe vào bãi vào database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **353**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `checked_in_by`, `entry_type`, `order_code`, `parking_orders.checked_in_by`, `parking_orders.created_at`, `parking_orders.entry_time`, `parking_orders.entry_type`, `parking_orders.license_plate`, `parking_orders.notes`, `parking_orders.order_code`, `parking_orders.parking_status`, `parking_orders.payment_status`, `parking_orders.subscription_id`, `parking_orders.updated_at`, `parking_orders.vehicle_id`, `parking_orders.vehicle_type_id`, `parking_orders.visitor_card_id`, `payment_status`, `visitor_card_id`

```sql
INSERT INTO parking_orders (
                        order_code,
                        vehicle_id,
                        vehicle_type_id,
                        subscription_id,
                        visitor_card_id,
                        license_plate,
                        entry_time,
                        parking_status,
                        entry_type,
                        payment_status,
                        checked_in_by,
                        notes,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, now(), 'ACTIVE', ?, ?, ?, ?, now(), now())
```

## SQL lưu trong biến Java

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:86`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đếm hoặc tổng hợp số liệu dashboard vận hành để phục vụ kiểm tra và dashboard.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **86**.

**Bảng/entity liên quan:** `parking_orders`, `vehicle_types`, `vehicles`, `visitor_cards`

**Cột/thuộc tính liên quan:** `calculated_fee`, `parking_orders.calculated_fee`, `parking_orders.entry_time`, `parking_orders.exit_time`, `parking_orders.notes`, `parking_orders.parking_status`, `parking_orders.vehicle_id`, `parking_orders.vehicle_type_id`, `vehicle_types.vehicle_type_id`, `vehicles.vehicle_id`, `vehicles.vehicle_type_id`

```sql
WITH params AS (
                    SELECT ?::date AS start_date, ?::date AS end_date
                ),
                order_metrics AS (
                    SELECT
                        COUNT(*) FILTER (
                            WHERE po.parking_status = 'ACTIVE'
                               OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL)
                        ) AS vehicles_in_parking,
                        COUNT(*) FILTER (
                            WHERE po.entry_time >= p.start_date AND po.entry_time < p.end_date
                        ) AS vehicles_in_today,
                        COUNT(*) FILTER (
                            WHERE po.entry_time >= p.start_date AND po.entry_time < p.end_date
                              AND %1$s = 'CAR'
                        ) AS vehicles_in_today_cars,
                        COUNT(*) FILTER (
                            WHERE po.entry_time >= p.start_date AND po.entry_time < p.end_date
                              AND %1$s = 'MOTORBIKE'
                        ) AS vehicles_in_today_motorbikes,
                        COUNT(*) FILTER (
                            WHERE po.exit_time >= p.start_date AND po.exit_time < p.end_date
                        ) AS vehicles_out_today,
                        COUNT(*) FILTER (
                            WHERE po.exit_time >= p.start_date AND po.exit_time < p.end_date
                              AND %1$s = 'CAR'
                        ) AS vehicles_out_today_cars,
                        COUNT(*) FILTER (
                            WHERE po.exit_time >= p.start_date AND po.exit_time < p.end_date
                              AND %1$s = 'MOTORBIKE'
                        ) AS vehicles_out_today_motorbikes,
                        COUNT(*) FILTER (
                            WHERE (po.parking_status = 'ACTIVE'
                                OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL))
                              AND %1$s = 'CAR'
                        ) AS active_cars,
                        COUNT(*) FILTER (
                            WHERE (po.parking_status = 'ACTIVE'
                                OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL))
                              AND %1$s = 'MOTORBIKE'
                        ) AS active_motorbikes,
                        COUNT(*) FILTER (
                            WHERE COALESCE(po.notes, '') <> ''
                              AND COALESCE(po.parking_status, '') IN ('ISSUE', 'EXCEPTION', 'OPEN', 'ACTIVE')
                        ) AS open_incidents,
                        COALESCE(SUM(po.calculated_fee) FILTER (
                            WHERE po.calculated_fee IS NOT NULL
                              AND po.exit_time >= p.start_date AND po.exit_time < p.end_date
                        ), 0) AS revenue_today
                    FROM parking_orders po
                    CROSS JOIN params p
                    LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                    LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                ),
                visitor_card_metrics AS (
                    SELECT COUNT(*) AS visitor_cards,
                           COUNT(*) FILTER (WHERE status = 'AVAILABLE') AS available_visitor_cards
                    FROM visitor_cards
                )
                SELECT *
                FROM order_metrics
                CROSS JOIN visitor_card_metrics
```

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:192`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc dashboard vận hành theo trạng thái nghiệp vụ; có thể kèm dữ liệu quan hệ để dựng response.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **192**.

**Bảng/entity liên quan:** `parking_facilities`, `parking_floors`, `parking_slots`

**Cột/thuộc tính liên quan:** `parking_facilities.created_at`, `parking_facilities.parking_id`, `parking_facilities.parking_name`, `parking_facilities.updated_at`, `parking_floors.created_at`, `parking_floors.floor_id`, `parking_floors.floor_name`, `parking_floors.floor_number`, `parking_floors.parking_id`, `parking_floors.updated_at`, `parking_slots.created_at`, `parking_slots.floor`, `parking_slots.id`, `parking_slots.slot_number`, `parking_slots.status`, `parking_slots.updated_at`

```sql
WITH default_facility AS (
                    SELECT parking_id, parking_name
                    FROM parking_facilities
                    ORDER BY parking_id
                    LIMIT 1
                ),
                first_floor AS (
                    SELECT DISTINCT ON (floor_number)
                        floor_id,
                        floor_name,
                        floor_number,
                        parking_id
                    FROM parking_floors
                    ORDER BY floor_number, parking_id
                )
                SELECT
                    ps.id,
                    ps.slot_number,
                    ps.floor,
                    ps.status,
                    ps.created_at,
                    ps.updated_at,
                    fl.floor_id,
                    fl.floor_name,
                    fl.floor_number,
                    COALESCE(pf.parking_id, df.parking_id) AS parking_id,
                    COALESCE(pf.parking_name, df.parking_name) AS parking_name
                FROM parking_slots ps
                LEFT JOIN first_floor ff ON ff.floor_number = ps.floor
                LEFT JOIN parking_floors fl ON fl.floor_id = ff.floor_id
                LEFT JOIN parking_facilities pf ON pf.parking_id = fl.parking_id
                LEFT JOIN default_facility df ON true
                ORDER BY COALESCE(pf.parking_id, df.parking_id, 0), ps.floor, ps.slot_number
```

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:403`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Tổng hợp giá trị dashboard vận hành, thường dùng cho thống kê doanh thu hoặc chỉ số vận hành.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **403**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `calculated_fee`, `parking_orders.calculated_fee`, `parking_orders.exit_time`

```sql
SELECT COALESCE(SUM(calculated_fee), 0)
                FROM parking_orders
                WHERE calculated_fee IS NOT NULL
                  AND exit_time >= ?
                  AND exit_time < ?
```

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:467`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc dashboard vận hành theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **467**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `order_id`, `parking_orders.created_at`, `parking_orders.license_plate`, `parking_orders.notes`, `parking_orders.order_id`, `parking_orders.parking_status`

```sql
SELECT
                    order_id,
                    license_plate,
                    parking_status,
                    notes,
                    created_at
                FROM parking_orders
                WHERE COALESCE(notes, '') <> ''
                ORDER BY created_at DESC
                LIMIT 5
```

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:501`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc dashboard vận hành theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **501**.

**Bảng/entity liên quan:** `parking_orders`, `vehicle_types`, `vehicles`, `visitor_cards`

**Cột/thuộc tính liên quan:** `calculated_fee`, `card_code`, `current_order_id`, `order_code`, `order_id`, `parking_orders.calculated_fee`, `parking_orders.card_id`, `parking_orders.entry_time`, `parking_orders.exit_time`, `parking_orders.license_plate`, `parking_orders.notes`, `parking_orders.order_code`, `parking_orders.order_id`, `parking_orders.parking_status`, `parking_orders.updated_at`, `parking_orders.vehicle_id`, `parking_orders.vehicle_type_id`, `parking_orders.visitor_card_id`, `vehicle_types.updated_at`, `vehicle_types.vehicle_type_id`, `vehicles.license_plate`, `vehicles.updated_at`, `vehicles.vehicle_id`, `vehicles.vehicle_type_id`, `visitor_card_id`, `visitor_cards.card_code`, `visitor_cards.current_order_id`, `visitor_cards.visitor_card_id`

```sql
SELECT
                    po.order_id,
                    po.order_code,
                    po.license_plate,
                    po.entry_time,
                    po.exit_time,
                    po.parking_status,
                    po.calculated_fee,
                    po.updated_at,
                    %s AS vehicle_type,
                    CASE
                        WHEN COALESCE(po.notes, '') LIKE 'ENTRY_TYPE=MONTHLY%%' THEN 'MONTHLY'
                        ELSE 'VISITOR'
                    END AS customer_type,
                    COALESCE(vc.card_code, substring(COALESCE(po.notes, '') from 'VISITOR_CARD=([^;]+)')) AS visitor_card_code,
                    NULL AS parking_name,
                    NULL AS floor_name
                FROM parking_orders po
                LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                LEFT JOIN visitor_cards vc ON vc.current_order_id = po.order_id OR vc.visitor_card_id = po.card_id
                ORDER BY po.updated_at DESC
                %s
```

### `src/main/java/com/swp/parking/service/ParkingAreaSummaryService.java:23`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc tổng hợp khu vực đỗ xe theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingAreaSummaryService.java`, bắt đầu tại dòng **23**.

**Bảng/entity liên quan:** `buildings`, `parking_facilities`, `parking_floors`, `parking_slots`

**Cột/thuộc tính liên quan:** `buildings.building_id`, `buildings.email`, `parking_facilities.building_id`, `parking_facilities.parking_id`, `parking_facilities.parking_name`, `parking_floors.floor_number`, `parking_floors.parking_id`, `parking_slots.floor`, `parking_slots.parking_id`

```sql
SELECT
                    pf.parking_id,
                    pf.parking_name,
                    CASE
                        WHEN LOWER(b.email) = 'cammy@sps.vn' THEN 'TCM'
                        WHEN LOWER(b.email) = 'bienhoa@sps.vn' THEN 'BH'
                        ELSE 'LK'
                    END AS building_code,
                    ARRAY_AGG(DISTINCT COALESCE(fl.floor_number, ps.floor) ORDER BY COALESCE(fl.floor_number, ps.floor)) AS floors
                FROM parking_facilities pf
                JOIN buildings b ON b.building_id = pf.building_id
                LEFT JOIN parking_floors fl ON fl.parking_id = pf.parking_id
                LEFT JOIN parking_slots ps ON ps.parking_id = pf.parking_id
                WHERE COALESCE(fl.floor_number, ps.floor) IS NOT NULL
                GROUP BY pf.parking_id, pf.parking_name, building_code
                ORDER BY pf.parking_id
```

### `src/main/java/com/swp/parking/service/ParkingAreaSummaryService.java:86`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đếm hoặc tổng hợp số liệu tổng hợp khu vực đỗ xe để phục vụ kiểm tra và dashboard.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingAreaSummaryService.java`, bắt đầu tại dòng **86**.

**Bảng/entity liên quan:** `area_codes`, `buildings`, `parking_facilities`, `parking_slots`

**Cột/thuộc tính liên quan:** `area_codes.area_code`, `buildings.building_id`, `buildings.building_name`, `buildings.email`, `buildings.status`, `parking_facilities.building_id`, `parking_facilities.parking_id`, `parking_facilities.parking_name`, `parking_slots.floor`, `parking_slots.id`, `parking_slots.parking_id`, `parking_slots.slot_number`, `parking_slots.status`

```sql
WITH area_codes(area_code) AS (
                    VALUES ('A'), ('B'), ('C'), ('D')
                ),
                selected_facility AS (
                    SELECT
                        pf.parking_id,
                        pf.parking_name,
                        ? AS building_code
                    FROM parking_facilities pf
                    JOIN buildings b ON b.building_id = pf.building_id
                    WHERE LOWER(b.email) = ?
                    LIMIT 1
                ),
                slot_areas AS (
                    SELECT
                        ps.id,
                        sf.building_code,
                        sf.parking_name AS building_name,
                        ps.floor AS floor_number,
                        ps.status,
                        COALESCE(
                            SUBSTRING(ps.slot_number FROM '^[A-Za-z]+-([A-Za-z])'),
                            SUBSTRING(ps.slot_number FROM '^([A-Za-z])'),
                            'A'
                        ) AS area_code,
                        CASE
                            WHEN ps.slot_number LIKE 'M-%' THEN 'MOTORBIKE'
                            WHEN SUBSTRING(ps.slot_number FROM '^[A-Za-z]+-([A-Za-z])') IN ('C', 'D') THEN 'MOTORBIKE'
                            WHEN SUBSTRING(ps.slot_number FROM '^([A-Za-z])') IN ('C', 'D') THEN 'MOTORBIKE'
                            ELSE 'CAR'
                        END AS vehicle_type
                    FROM parking_slots ps
                    JOIN selected_facility sf ON sf.parking_id = ps.parking_id
                    WHERE (? IS NULL OR ps.floor = ?)
                ),
                slot_summary AS (
                    SELECT
                        building_code,
                        building_name,
                        floor_number,
                        area_code,
                        CASE
                            WHEN COUNT(*) FILTER (WHERE vehicle_type = 'MOTORBIKE') > COUNT(*) FILTER (WHERE vehicle_type = 'CAR')
                                THEN 'MOTORBIKE'
                            ELSE 'CAR'
                        END AS vehicle_type,
                        COUNT(*)::int AS capacity,
                        COUNT(*) FILTER (WHERE status IN ('OCCUPIED', 'RESERVED'))::int AS current_vehicle_count,
                        COUNT(*) FILTER (WHERE status = 'AVAILABLE')::int AS available_count,
                        COUNT(*) FILTER (WHERE status = 'MAINTENANCE')::int AS maintenance_count,
                        MIN(id) AS area_id
                    FROM slot_areas
                    GROUP BY building_code, building_name, floor_number, area_code
                )
                SELECT
                    COALESCE(ss.area_id, ROW_NUMBER() OVER (ORDER BY ac.area_code) * -1) AS area_id,
                    sf.building_code,
                    sf.parking_name AS building_name,
                    ? AS floor_number,
                    ac.area_code,
                    COALESCE(ss.vehicle_type, CASE WHEN ac.area_code IN ('C', 'D') THEN 'MOTORBIKE' ELSE 'CAR' END) AS vehicle_type,
                    COALESCE(ss.capacity, 0) AS capacity,
                    COALESCE(ss.current_vehicle_count, 0) AS current_vehicle_count,
                    COALESCE(ss.available_count, 0) AS available_count,
                    COALESCE(ss.maintenance_count, 0) AS maintenance_count
                FROM selected_facility sf
                CROSS JOIN area_codes ac
                LEFT JOIN slot_summary ss ON ss.area_code = ac.area_code
                ORDER BY ac.area_code
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:310`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc và khóa bản ghi xe vào bãi để tránh hai request xử lý đồng thời cùng một dữ liệu.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **310**.

**Bảng/entity liên quan:** `SKIP`, `visitor_cards`

**Cột/thuộc tính liên quan:** `card_code`, `display_number`, `visitor_card_id`

```sql
SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE status = 'AVAILABLE'
                    ORDER BY display_number
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:320`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc và khóa bản ghi xe vào bãi để tránh hai request xử lý đồng thời cùng một dữ liệu.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **320**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** `card_code`, `display_number`, `visitor_card_id`

```sql
SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE card_code = ?
                      AND status = 'AVAILABLE'
                    LIMIT 1
                    FOR UPDATE
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:136`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc cấu hình, sự cố và audit hệ thống thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **136**.

**Bảng/entity liên quan:** `parking_incidents`, `users`

**Cột/thuộc tính liên quan:** `created_by`, `description`, `incident_id`, `incident_type`, `parking_incidents.created_at`, `parking_incidents.created_by`, `parking_incidents.description`, `parking_incidents.incident_id`, `parking_incidents.incident_type`, `parking_incidents.reply_title`, `parking_incidents.resolution`, `parking_incidents.resolved_at`, `parking_incidents.resolved_by`, `parking_incidents.status`, `reply_title`, `resolution`, `resolved_at`, `resolved_by`, `users.created_at`, `users.email`, `users.full_name`, `users.user_id`

```sql
SELECT i.incident_id AS id,
                       u.full_name AS "userName",
                       u.email AS "userEmail",
                       i.incident_type AS service,
                       i.description AS content,
                       i.status,
                       i.created_at AS "createdAt",
                       i.reply_title AS "replyTitle",
                       i.resolution AS "replyMessage",
                       resolver.full_name AS "repliedBy",
                       i.resolved_at AS "repliedAt"
                FROM parking_incidents i
                LEFT JOIN users u ON u.user_id = i.created_by
                LEFT JOIN users resolver ON resolver.user_id = i.resolved_by
```

## SQL trả về từ hàm helper

### `src/main/java/com/swp/parking/service/ParkingExitService.java:142`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc xe ra bãi thuộc một người dùng cụ thể; các bảng và cột dùng để lọc/ghép được liệt kê bên dưới.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingExitService.java`, bắt đầu tại dòng **142**.

**Bảng/entity liên quan:** `customers`, `fee_package`, `fee_subscription`, `parking_orders`, `users`, `vehicle_types`, `vehicles`, `visitor_cards`

**Cột/thuộc tính liên quan:** `card_code`, `current_order_id`, `customers.customer_id`, `customers.user_id`, `entry_type`, `fee_package.fee_package_id`, `fee_package.name`, `fee_package.vehicle_type_id`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.vehicle_id`, `order_code`, `order_id`, `parking_orders.entry_time`, `parking_orders.entry_type`, `parking_orders.license_plate`, `parking_orders.notes`, `parking_orders.order_code`, `parking_orders.order_id`, `parking_orders.parking_id`, `parking_orders.parking_status`, `parking_orders.subscription_id`, `parking_orders.vehicle_id`, `parking_orders.vehicle_type_id`, `parking_orders.visitor_card_id`, `users.full_name`, `users.user_id`, `vehicle_types.type_name`, `vehicle_types.vehicle_type_id`, `vehicles.brand`, `vehicles.color`, `vehicles.customer_id`, `vehicles.license_plate`, `vehicles.vehicle_id`, `vehicles.vehicle_type_id`, `visitor_card_id`, `visitor_cards.card_code`, `visitor_cards.current_order_id`, `visitor_cards.visitor_card_id`

```sql
SELECT po.order_id,
                       po.order_code,
                       po.license_plate,
                       po.vehicle_id,
                       po.vehicle_type_id,
                       po.parking_id,
                       po.entry_time,
                       po.parking_status,
                       COALESCE(po.entry_type,
                           CASE WHEN po.notes LIKE 'ENTRY_TYPE=MONTHLY%%' THEN 'SUBSCRIPTION' ELSE 'VISITOR' END
                       ) AS entry_type,
                       v.brand,
                       v.color,
                       vt.type_name AS vehicle_type,
                       u.full_name AS customer_name,
                       vc.visitor_card_id,
                       vc.card_code AS visitor_card_code,
                       fs.fee_subscription_id AS subscription_id,
                       fs.start_date AS subscription_start_date,
                       fs.end_date AS subscription_end_date,
                       fs.status AS subscription_status,
                       fp.name AS package_name
                  FROM parking_orders po
                  LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                  LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = COALESCE(po.vehicle_type_id, v.vehicle_type_id)
                  LEFT JOIN customers c ON c.customer_id = v.customer_id
                  LEFT JOIN users u ON u.user_id = c.user_id
                  LEFT JOIN visitor_cards vc
                    ON vc.visitor_card_id = po.visitor_card_id
                    OR (po.visitor_card_id IS NULL AND vc.current_order_id = po.order_id)
                  LEFT JOIN fee_subscription fs ON fs.fee_subscription_id = po.subscription_id
                  LEFT JOIN fee_package fp ON fp.fee_package_id = fs.fee_package_id
```

## SQL truyền vào hàm helper

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:338`

**Loại query:** Native SQL PostgreSQL được thực thi từ Java.

**Mục đích:** Đọc dashboard vận hành theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **338**.

**Bảng/entity liên quan:** Không xác định tự động; xem entity/method tại vị trí source phía trên.

**Cột/thuộc tính liên quan:** Không ghi cứng trong source; Hibernate quyết định theo entity mapping hoặc câu query động.

```sql
, date, date.plusDays(1));
    }

    private long countVehiclesInTodayByType(LocalDate date, String vehicleType) {
        return queryLong(
```

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:359`

**Loại query:** Native SQL PostgreSQL được thực thi từ Java.

**Mục đích:** Đọc dashboard vận hành theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **359**.

**Bảng/entity liên quan:** Không xác định tự động; xem entity/method tại vị trí source phía trên.

**Cột/thuộc tính liên quan:** Không ghi cứng trong source; Hibernate quyết định theo entity mapping hoặc câu query động.

```sql
, date, date.plusDays(1));
    }

    private long countVehiclesOutTodayByType(LocalDate date, String vehicleType) {
        return queryLong(
```

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:382`

**Loại query:** Native SQL PostgreSQL được thực thi từ Java.

**Mục đích:** Đếm hoặc tổng hợp số liệu dashboard vận hành để phục vụ kiểm tra và dashboard.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **382**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** Không ghi cứng trong source; Hibernate quyết định theo entity mapping hoặc câu query động.

```sql
.formatted(vehicleTypeExpression()), vehicleType);
    }

    private long countVisitorCards() {
        return queryLong("SELECT COUNT(*) FROM visitor_cards");
    }

    private long countAvailableVisitorCards() {
        return queryLong("SELECT COUNT(*) FROM visitor_cards WHERE status = 'AVAILABLE'");
    }

    private long countOpenIncidents() {
        return queryLong(
```

## SQL chuỗi một dòng

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:386`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đếm hoặc tổng hợp số liệu dashboard vận hành để phục vụ kiểm tra và dashboard.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **386**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** Không ghi cứng trong source; Hibernate quyết định theo entity mapping hoặc câu query động.

```sql
SELECT COUNT(*) FROM visitor_cards
```

### `src/main/java/com/swp/parking/service/OperationsDashboardService.java:390`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đếm hoặc tổng hợp số liệu dashboard vận hành để phục vụ kiểm tra và dashboard.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/OperationsDashboardService.java`, bắt đầu tại dòng **390**.

**Bảng/entity liên quan:** `visitor_cards`

**Cột/thuộc tính liên quan:** Không ghi cứng trong source; Hibernate quyết định theo entity mapping hoặc câu query động.

```sql
SELECT COUNT(*) FROM visitor_cards WHERE status = 'AVAILABLE'
```

### `src/main/java/com/swp/parking/service/ParkingEntryService.java:407`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc xe vào bãi theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingEntryService.java`, bắt đầu tại dòng **407**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `order_code`, `order_id`, `parking_orders.order_code`, `parking_orders.order_id`

```sql
SELECT order_id FROM parking_orders WHERE order_code = ?
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:28`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Đọc cấu hình, sự cố và audit hệ thống theo các điều kiện, phép nối và thứ tự thể hiện trong câu query.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **28**.

**Bảng/entity liên quan:** `system_configuration`

**Cột/thuộc tính liên quan:** `config_data`, `config_id`

```sql
SELECT config_data::text FROM system_configuration WHERE config_id = 1
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:85`

**Loại query:** Native SQL PostgreSQL dạng `SELECT` qua `JdbcTemplate`; chỉ đọc.

**Mục đích:** Lấy chi tiết cấu hình, sự cố và audit hệ thống theo mã định danh được truyền vào.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **85**.

**Bảng/entity liên quan:** `parking_incidents`

**Cột/thuộc tính liên quan:** `created_by`, `incident_id`

```sql
SELECT created_by FROM parking_incidents WHERE incident_id = ?
```

### `src/main/java/com/swp/parking/service/SystemDataService.java:179`

**Loại query:** Native SQL PostgreSQL dạng `INSERT` qua `JdbcTemplate`.

**Mục đích:** Thêm dữ liệu cấu hình, sự cố và audit hệ thống vào database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SystemDataService.java`, bắt đầu tại dòng **179**.

**Bảng/entity liên quan:** `audit_logs`

**Cột/thuộc tính liên quan:** `action`, `message`, `severity`

```sql
INSERT INTO audit_logs(user_id, action, status, severity, message) VALUES (?, ?, ?, ?, ?)
```

## Query Spring Data sinh từ tên method

### `src/main/java/com/swp/parking/repository/CardRepository.java:12`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc thẻ gửi xe theo bộ lọc và thứ tự được mã hóa trong tên method `findByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/CardRepository.java`, bắt đầu tại dòng **12**.

**Bảng/entity liên quan:** `cards`

**Cột/thuộc tính liên quan:** `cards.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<Card> findByUserId(Long userId);
```

### `src/main/java/com/swp/parking/repository/CardRepository.java:14`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của thẻ gửi xe theo điều kiện mã hóa trong tên method `existsByCardCode`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/CardRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `cards`

**Cột/thuộc tính liên quan:** `cards.card_code`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByCardCode(String cardCode);
```

### `src/main/java/com/swp/parking/repository/CustomerRepository.java:10`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc khách hàng theo bộ lọc và thứ tự được mã hóa trong tên method `findByUser_Id`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/CustomerRepository.java`, bắt đầu tại dòng **10**.

**Bảng/entity liên quan:** `customers`

**Cột/thuộc tính liên quan:** `customers.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<Customer> findByUser_Id(Long userId);
```

### `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java:16`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc token thiết bị theo bộ lọc và thứ tự được mã hóa trong tên method `findByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java`, bắt đầu tại dòng **16**.

**Bảng/entity liên quan:** `device_tokens`

**Cột/thuộc tính liên quan:** `device_tokens.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<DeviceToken> findByUserId(Long userId);
```

### `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java:18`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc token thiết bị theo bộ lọc và thứ tự được mã hóa trong tên method `findByToken`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java`, bắt đầu tại dòng **18**.

**Bảng/entity liên quan:** `device_tokens`

**Cột/thuộc tính liên quan:** `device_tokens.token`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<DeviceToken> findByToken(String token);
```

### `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java:22`

**Loại query:** Spring Data Derived Query dạng `DELETE`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Xóa token thiết bị theo điều kiện mã hóa trong tên method `deleteByToken`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/DeviceTokenRepository.java`, bắt đầu tại dòng **22**.

**Bảng/entity liên quan:** `device_tokens`

**Cột/thuộc tính liên quan:** `device_tokens.token`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
int deleteByToken(@Param("token") String token);
```

### `src/main/java/com/swp/parking/repository/FeePackagePriceHistoryRepository.java:11`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc lịch sử giá gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findFirstByFeePackage_IdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeePackagePriceHistoryRepository.java`, bắt đầu tại dòng **11**.

**Bảng/entity liên quan:** `fee_package_price_history`

**Cột/thuộc tính liên quan:** `fee_package_price_history.effective_from`, `fee_package_price_history.fee_package_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<FeePackagePriceHistory> findFirstByFeePackage_IdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc( Long feePackageId, LocalDateTime dateTime);
```

### `src/main/java/com/swp/parking/repository/FeePackagePriceHistoryRepository.java:14`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc lịch sử giá gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findFirstByFeePackage_IdAndEffectiveToIsNullOrderByEffectiveFromDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeePackagePriceHistoryRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `fee_package_price_history`

**Cột/thuộc tính liên quan:** `fee_package_price_history.effective_from`, `fee_package_price_history.effective_to`, `fee_package_price_history.fee_package_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<FeePackagePriceHistory> findFirstByFeePackage_IdAndEffectiveToIsNullOrderByEffectiveFromDesc( Long feePackageId);
```

### `src/main/java/com/swp/parking/repository/FeePackageRepository.java:14`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findByIsActiveTrue`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeePackageRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `fee_package`

**Cột/thuộc tính liên quan:** `fee_package.is_active`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeePackage> findByIsActiveTrue();
```

### `src/main/java/com/swp/parking/repository/FeePackageRepository.java:17`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findByVehicleType_IdAndIsActiveTrue`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeePackageRepository.java`, bắt đầu tại dòng **17**.

**Bảng/entity liên quan:** `fee_package`

**Cột/thuộc tính liên quan:** `fee_package.is_active`, `fee_package.vehicle_type_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeePackage> findByVehicleType_IdAndIsActiveTrue(@Param("vehicleTypeId") Long vehicleTypeId);
```

### `src/main/java/com/swp/parking/repository/FeePackageRepository.java:47`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findActiveWithCurrentPrice`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeePackageRepository.java`, bắt đầu tại dòng **47**.

**Bảng/entity liên quan:** `fee_package`

**Cột/thuộc tính liên quan:** `fee_package.benefits`, `fee_package.created_at`, `fee_package.duration_months`, `fee_package.fee_package_id`, `fee_package.is_active`, `fee_package.is_best_value`, `fee_package.is_popular`, `fee_package.name`, `fee_package.updated_at`, `fee_package.vehicle_type_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeePackageWithCurrentPrice> findActiveWithCurrentPrice( @Param("vehicleTypeId") Long vehicleTypeId, @Param("at") LocalDateTime at);
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionInvoiceRepository.java:12`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc hóa đơn gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findByFeeSubscriptionIdOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionInvoiceRepository.java`, bắt đầu tại dòng **12**.

**Bảng/entity liên quan:** `fee_subscription_invoice`

**Cột/thuộc tính liên quan:** `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeeSubscriptionInvoice> findByFeeSubscriptionIdOrderByCreatedAtDesc(Long feeSubscriptionId);
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionInvoiceRepository.java:22`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc hóa đơn gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findAllByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionInvoiceRepository.java`, bắt đầu tại dòng **22**.

**Bảng/entity liên quan:** `fee_subscription_invoice`

**Cột/thuộc tính liên quan:** `fee_subscription_invoice.amount`, `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`, `fee_subscription_invoice.invoice_id`, `fee_subscription_invoice.message`, `fee_subscription_invoice.momo_order_id`, `fee_subscription_invoice.momo_request_id`, `fee_subscription_invoice.momo_trans_id`, `fee_subscription_invoice.status`, `fee_subscription_invoice.type`, `fee_subscription_invoice.updated_at`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeeSubscriptionInvoice> findAllByUserId(@Param("userId") Long userId);
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java:15`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java`, bắt đầu tại dòng **15**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeeSubscription> findByUserId(@Param("userId") Long userId);
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java:17`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của đăng ký gói phí theo điều kiện mã hóa trong tên method `existsByVehicle_IdAndStatus`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java`, bắt đầu tại dòng **17**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.status`, `fee_subscription.vehicle_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByVehicle_IdAndStatus(Long vehicleId, SubscriptionStatus status);
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java:19`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findByVehicle_IdAndStatus`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java`, bắt đầu tại dòng **19**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.status`, `fee_subscription.vehicle_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<FeeSubscription> findByVehicle_IdAndStatus(Long vehicleId, SubscriptionStatus status);
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java:21`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findAllByVehicle_IdAndStatus`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java`, bắt đầu tại dòng **21**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.status`, `fee_subscription.vehicle_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeeSubscription> findAllByVehicle_IdAndStatus(Long vehicleId, SubscriptionStatus status);
```

### `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java:31`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký gói phí theo bộ lọc và thứ tự được mã hóa trong tên method `findAllByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/FeeSubscriptionRepository.java`, bắt đầu tại dòng **31**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<FeeSubscription> findAllByUserId(@Param("userId") Long userId);
```

### `src/main/java/com/swp/parking/repository/MomoOrderRepository.java:12`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đơn thanh toán MoMo theo bộ lọc và thứ tự được mã hóa trong tên method `findFirstBySubscriptionIdOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/MomoOrderRepository.java`, bắt đầu tại dòng **12**.

**Bảng/entity liên quan:** `momo_order`

**Cột/thuộc tính liên quan:** `momo_order.created_at`, `momo_order.subscription_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<MomoOrder> findFirstBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
```

### `src/main/java/com/swp/parking/repository/MomoOrderRepository.java:14`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đơn thanh toán MoMo theo bộ lọc và thứ tự được mã hóa trong tên method `findByUserIdOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/MomoOrderRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `momo_order`

**Cột/thuộc tính liên quan:** `momo_order.created_at`, `momo_order.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<MomoOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
```

### `src/main/java/com/swp/parking/repository/MomoOrderRepository.java:16`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đơn thanh toán MoMo theo bộ lọc và thứ tự được mã hóa trong tên method `findByPaymentStatusOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/MomoOrderRepository.java`, bắt đầu tại dòng **16**.

**Bảng/entity liên quan:** `momo_order`

**Cột/thuộc tính liên quan:** `momo_order.created_at`, `momo_order.payment_status`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<MomoOrder> findByPaymentStatusOrderByCreatedAtDesc(MomoOrderStatus status);
```

### `src/main/java/com/swp/parking/repository/NotificationRepository.java:25`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc thông báo theo bộ lọc và thứ tự được mã hóa trong tên method `findPublicNotifications`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/NotificationRepository.java`, bắt đầu tại dòng **25**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.category`, `notifications.content`, `notifications.created_at`, `notifications.created_by`, `notifications.id`, `notifications.is_active`, `notifications.priority`, `notifications.published_at`, `notifications.recipient_target`, `notifications.recipient_user_id`, `notifications.scheduled_at`, `notifications.status`, `notifications.summary`, `notifications.title`, `notifications.updated_at`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Page<Notification> findPublicNotifications(@Param("status") NotificationStatus status, Pageable pageable);
```

### `src/main/java/com/swp/parking/repository/NotificationRepository.java:28`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc thông báo theo bộ lọc và thứ tự được mã hóa trong tên method `findByIsActiveTrueAndStatusOrderByPublishedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/NotificationRepository.java`, bắt đầu tại dòng **28**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.is_active`, `notifications.published_at`, `notifications.status`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<Notification> findByIsActiveTrueAndStatusOrderByPublishedAtDesc(NotificationStatus status);
```

### `src/main/java/com/swp/parking/repository/NotificationRepository.java:37`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc thông báo theo bộ lọc và thứ tự được mã hóa trong tên method `findVisibleForUser`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/NotificationRepository.java`, bắt đầu tại dòng **37**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.category`, `notifications.content`, `notifications.created_at`, `notifications.created_by`, `notifications.id`, `notifications.is_active`, `notifications.priority`, `notifications.published_at`, `notifications.recipient_target`, `notifications.recipient_user_id`, `notifications.scheduled_at`, `notifications.status`, `notifications.summary`, `notifications.title`, `notifications.updated_at`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<Notification> findVisibleForUser(@Param("userId") Long userId, @Param("status") NotificationStatus status);
```

### `src/main/java/com/swp/parking/repository/NotificationRepository.java:40`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc thông báo theo bộ lọc và thứ tự được mã hóa trong tên method `findAllByOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/NotificationRepository.java`, bắt đầu tại dòng **40**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.created_at`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<Notification> findAllByOrderByCreatedAtDesc();
```

### `src/main/java/com/swp/parking/repository/ParkingFloorRepository.java:12`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc tầng đỗ xe theo bộ lọc và thứ tự được mã hóa trong tên method `findByParkingFacilityIdOrderByFloorNumberAscIdAsc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingFloorRepository.java`, bắt đầu tại dòng **12**.

**Bảng/entity liên quan:** `parking_floors`

**Cột/thuộc tính liên quan:** `parking_floors.floor_number`, `parking_floors.parking_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<ParkingFloor> findByParkingFacilityIdOrderByFloorNumberAscIdAsc(Long parkingId);
```

### `src/main/java/com/swp/parking/repository/ParkingOrderRepository.java:22`

**Loại query:** Spring Data Derived Query dạng `COUNT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đếm số bản ghi phiên/đơn gửi xe theo điều kiện mã hóa trong tên method `countActiveVehicles`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingOrderRepository.java`, bắt đầu tại dòng **22**.

**Bảng/entity liên quan:** `parking_orders`

**Cột/thuộc tính liên quan:** `parking_orders.vehicle_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
long countActiveVehicles( @Param("parkingId") Long parkingId, @Param("floorId") Long floorId, @Param("vehicleTypeId") Long vehicleTypeId );
```

### `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java:14`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc ô đỗ xe theo bộ lọc và thứ tự được mã hóa trong tên method `findByStatus`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.status`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<ParkingSlot> findByStatus(ParkingSlotStatus status);
```

### `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java:16`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc ô đỗ xe theo bộ lọc và thứ tự được mã hóa trong tên method `findBySlotNumber`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java`, bắt đầu tại dòng **16**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.slot_number`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<ParkingSlot> findBySlotNumber(String slotNumber);
```

### `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java:18`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của ô đỗ xe theo điều kiện mã hóa trong tên method `existsBySlotNumberAndFloor`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java`, bắt đầu tại dòng **18**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.floor`, `parking_slots.slot_number`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsBySlotNumberAndFloor(String slotNumber, Integer floor);
```

### `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java:20`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc ô đỗ xe theo bộ lọc và thứ tự được mã hóa trong tên method `findByFloor`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingSlotRepository.java`, bắt đầu tại dòng **20**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.floor`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<ParkingSlot> findByFloor(Integer floor);
```

### `src/main/java/com/swp/parking/repository/ParkingZoneRepository.java:13`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc khu vực đỗ xe theo bộ lọc và thứ tự được mã hóa trong tên method `findByParkingFacilityIdOrderByIdAsc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingZoneRepository.java`, bắt đầu tại dòng **13**.

**Bảng/entity liên quan:** `parking_zones`

**Cột/thuộc tính liên quan:** `parking_zones.parking_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<ParkingZone> findByParkingFacilityIdOrderByIdAsc(Long parkingId);
```

### `src/main/java/com/swp/parking/repository/ParkingZoneRepository.java:15`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc khu vực đỗ xe theo bộ lọc và thứ tự được mã hóa trong tên method `findByParkingFacilityIdAndParkingFloorIdOrderByIdAsc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingZoneRepository.java`, bắt đầu tại dòng **15**.

**Bảng/entity liên quan:** `parking_zones`

**Cột/thuộc tính liên quan:** `parking_zones.floor_id`, `parking_zones.parking_id`, `parking_zones.zone_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<ParkingZone> findByParkingFacilityIdAndParkingFloorIdOrderByIdAsc(Long parkingId, Long floorId);
```

### `src/main/java/com/swp/parking/repository/ParkingZoneRepository.java:17`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc khu vực đỗ xe theo bộ lọc và thứ tự được mã hóa trong tên method `findByParkingFacilityIdAndParkingFloorIdAndVehicleTypeId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/ParkingZoneRepository.java`, bắt đầu tại dòng **17**.

**Bảng/entity liên quan:** `parking_zones`

**Cột/thuộc tính liên quan:** `parking_zones.floor_id`, `parking_zones.parking_id`, `parking_zones.vehicle_type_id`, `parking_zones.zone_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<ParkingZone> findByParkingFacilityIdAndParkingFloorIdAndVehicleTypeId( Long parkingId, Long floorId, Long vehicleTypeId );
```

### `src/main/java/com/swp/parking/repository/UserRepository.java:14`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc người dùng theo bộ lọc và thứ tự được mã hóa trong tên method `findByEmail`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/UserRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.email`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<User> findByEmail(String email);
```

### `src/main/java/com/swp/parking/repository/UserRepository.java:16`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của người dùng theo điều kiện mã hóa trong tên method `existsByEmail`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/UserRepository.java`, bắt đầu tại dòng **16**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.email`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByEmail(String email);
```

### `src/main/java/com/swp/parking/repository/UserRepository.java:19`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc người dùng theo bộ lọc và thứ tự được mã hóa trong tên method `findActiveEmployeeRoleByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/UserRepository.java`, bắt đầu tại dòng **19**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.role`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<String> findActiveEmployeeRoleByUserId(@Param("userId") Long userId);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:15`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findByUser_IdOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **15**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.created_at`, `vehicle_registrations.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<VehicleRegistration> findByUser_IdOrderByCreatedAtDesc(Long userId);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:17`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findAllByOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **17**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.created_at`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Page<VehicleRegistration> findAllByOrderByCreatedAtDesc(Pageable pageable);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:19`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findByStatusOrderByCreatedAtDesc`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **19**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.created_at`, `vehicle_registrations.status`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Page<VehicleRegistration> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:52`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findSummaries`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **52**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.brand`, `vehicle_registrations.cccd_back_image`, `vehicle_registrations.cccd_front_image`, `vehicle_registrations.color`, `vehicle_registrations.contact_phone`, `vehicle_registrations.created_at`, `vehicle_registrations.ekyc_cccd_expiry_date`, `vehicle_registrations.ekyc_cccd_id`, `vehicle_registrations.ekyc_cccd_issue_date`, `vehicle_registrations.ekyc_confidence_score`, `vehicle_registrations.ekyc_date_of_birth`, `vehicle_registrations.ekyc_document_type`, `vehicle_registrations.ekyc_face_match_score`, `vehicle_registrations.ekyc_full_name`, `vehicle_registrations.ekyc_gender`, `vehicle_registrations.ekyc_is_fake`, `vehicle_registrations.ekyc_is_valid`, `vehicle_registrations.ekyc_issuing_authority`, `vehicle_registrations.ekyc_license_class`, `vehicle_registrations.ekyc_license_expiry`, `vehicle_registrations.ekyc_license_issue_date`, `vehicle_registrations.ekyc_license_number`, `vehicle_registrations.ekyc_nationality`, `vehicle_registrations.ekyc_place_of_origin`, `vehicle_registrations.ekyc_place_of_residence`, `vehicle_registrations.license_image`, `vehicle_registrations.license_plate`, `vehicle_registrations.plate_image`, `vehicle_registrations.registration_id`, `vehicle_registrations.registration_source`, `vehicle_registrations.reject_reason`, `vehicle_registrations.requested_fee_package_id`, `vehicle_registrations.reviewed_at`, `vehicle_registrations.reviewed_by`, `vehicle_registrations.status`, `vehicle_registrations.updated_at`, `vehicle_registrations.user_id`, `vehicle_registrations.vehicle_document_image`, `vehicle_registrations.vehicle_id`, `vehicle_registrations.vehicle_type_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Page<VehicleRegistrationSummary> findSummaries(@Param("status") String status, Pageable pageable);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:85`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc đăng ký phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findSummariesByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **85**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<VehicleRegistrationSummary> findSummariesByUserId(@Param("userId") Long userId);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:87`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của đăng ký phương tiện theo điều kiện mã hóa trong tên method `existsByUser_IdAndLicensePlate`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **87**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.license_plate`, `vehicle_registrations.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByUser_IdAndLicensePlate(Long userId, String licensePlate);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:89`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của đăng ký phương tiện theo điều kiện mã hóa trong tên method `existsByUser_IdAndLicensePlateAndStatusIn`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **89**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.license_plate`, `vehicle_registrations.status`, `vehicle_registrations.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByUser_IdAndLicensePlateAndStatusIn(Long userId, String licensePlate, List<String> statuses);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:91`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của đăng ký phương tiện theo điều kiện mã hóa trong tên method `existsByEkycCccdIdAndUser_IdNot`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **91**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.ekyc_cccd_id`, `vehicle_registrations.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByEkycCccdIdAndUser_IdNot(String ekycCccdId, Long userId);
```

### `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java:93`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của đăng ký phương tiện theo điều kiện mã hóa trong tên method `existsByEkycLicenseNumberAndUser_IdNot`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRegistrationRepository.java`, bắt đầu tại dòng **93**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.ekyc_license_number`, `vehicle_registrations.user_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByEkycLicenseNumberAndUser_IdNot(String ekycLicenseNumber, Long userId);
```

### `src/main/java/com/swp/parking/repository/VehicleRepository.java:14`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findByUserId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRepository.java`, bắt đầu tại dòng **14**.

**Bảng/entity liên quan:** `vehicles`

**Cột/thuộc tính liên quan:** `vehicles.brand`, `vehicles.color`, `vehicles.created_at`, `vehicles.customer_id`, `vehicles.license_plate`, `vehicles.updated_at`, `vehicles.vehicle_id`, `vehicles.vehicle_type_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<Vehicle> findByUserId(@Param("userId") Long userId);
```

### `src/main/java/com/swp/parking/repository/VehicleRepository.java:17`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findByUserIdAndVehicleTypeId`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRepository.java`, bắt đầu tại dòng **17**.

**Bảng/entity liên quan:** `vehicles`

**Cột/thuộc tính liên quan:** `vehicles.vehicle_type_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
List<Vehicle> findByUserIdAndVehicleTypeId(@Param("userId") Long userId, @Param("vehicleTypeId") Long vehicleTypeId);
```

### `src/main/java/com/swp/parking/repository/VehicleRepository.java:19`

**Loại query:** Spring Data Derived Query dạng `EXISTS`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Kiểm tra sự tồn tại của phương tiện theo điều kiện mã hóa trong tên method `existsByIdAndCustomer_User_Id`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRepository.java`, bắt đầu tại dòng **19**.

**Bảng/entity liên quan:** `vehicles`

**Cột/thuộc tính liên quan:** `vehicles.customer_id`, `vehicles.vehicle_id`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
boolean existsByIdAndCustomer_User_Id(Long vehicleId, Long userId);
```

### `src/main/java/com/swp/parking/repository/VehicleRepository.java:21`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findByLicensePlate`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleRepository.java`, bắt đầu tại dòng **21**.

**Bảng/entity liên quan:** `vehicles`

**Cột/thuộc tính liên quan:** `vehicles.license_plate`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<Vehicle> findByLicensePlate(String licensePlate);
```

### `src/main/java/com/swp/parking/repository/VehicleTypeRepository.java:11`

**Loại query:** Spring Data Derived Query dạng `SELECT`; Hibernate tự sinh SQL từ tên method.

**Mục đích:** Đọc loại phương tiện theo bộ lọc và thứ tự được mã hóa trong tên method `findFirstByTypeCodeIgnoreCase`.

**Vị trí trong source:** `src/main/java/com/swp/parking/repository/VehicleTypeRepository.java`, bắt đầu tại dòng **11**.

**Bảng/entity liên quan:** `vehicle_types`

**Cột/thuộc tính liên quan:** `vehicle_types.type_code`

**Ghi chú:**

SQL thực tế do Hibernate sinh dựa trên tên method và mapping entity.

```java
Optional<VehicleType> findFirstByTypeCodeIgnoreCase(String typeCode);
```

## Thao tác CRUD kế thừa từ JpaRepository

### `src/main/java/com/swp/parking/controller/VehicleTypeController.java:22`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy toàn bộ loại phương tiện từ database.

**Vị trí trong source:** `src/main/java/com/swp/parking/controller/VehicleTypeController.java`, bắt đầu tại dòng **22**.

**Bảng/entity liên quan:** `vehicle_types`

**Cột/thuộc tính liên quan:** `vehicle_types.created_at`, `vehicle_types.type_code`, `vehicle_types.type_name`, `vehicle_types.updated_at`, `vehicle_types.vehicle_type_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return ResponseEntity.ok(vehicleTypeRepository.findAll().stream()
```

### `src/main/java/com/swp/parking/service/AdminNotificationService.java:56`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/AdminNotificationService.java`, bắt đầu tại dòng **56**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.category`, `notifications.content`, `notifications.created_at`, `notifications.created_by`, `notifications.id`, `notifications.is_active`, `notifications.priority`, `notifications.published_at`, `notifications.recipient_target`, `notifications.recipient_user_id`, `notifications.scheduled_at`, `notifications.status`, `notifications.summary`, `notifications.title`, `notifications.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
Notification saved = notificationRepository.save(notification);
```

### `src/main/java/com/swp/parking/service/AdminNotificationService.java:75`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/AdminNotificationService.java`, bắt đầu tại dòng **75**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.category`, `notifications.content`, `notifications.created_at`, `notifications.created_by`, `notifications.id`, `notifications.is_active`, `notifications.priority`, `notifications.published_at`, `notifications.recipient_target`, `notifications.recipient_user_id`, `notifications.scheduled_at`, `notifications.status`, `notifications.summary`, `notifications.title`, `notifications.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
Notification saved = notificationRepository.save(notification);
```

### `src/main/java/com/swp/parking/service/AdminNotificationService.java:84`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/AdminNotificationService.java`, bắt đầu tại dòng **84**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.category`, `notifications.content`, `notifications.created_at`, `notifications.created_by`, `notifications.id`, `notifications.is_active`, `notifications.priority`, `notifications.published_at`, `notifications.recipient_target`, `notifications.recipient_user_id`, `notifications.scheduled_at`, `notifications.status`, `notifications.summary`, `notifications.title`, `notifications.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
Notification saved = notificationRepository.save(notification);
```

### `src/main/java/com/swp/parking/service/AdminNotificationService.java:94`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/AdminNotificationService.java`, bắt đầu tại dòng **94**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return notificationRepository.findById(id)
```

### `src/main/java/com/swp/parking/service/AuthService.java:65`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/AuthService.java`, bắt đầu tại dòng **65**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.created_at`, `users.email`, `users.full_name`, `users.password_hash`, `users.role`, `users.updated_at`, `users.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
user = userRepository.save(user);
```

### `src/main/java/com/swp/parking/service/AuthService.java:116`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/AuthService.java`, bắt đầu tại dòng **116**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.created_at`, `users.email`, `users.full_name`, `users.password_hash`, `users.role`, `users.updated_at`, `users.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return userRepository.save(newUser);
```

### `src/main/java/com/swp/parking/service/CardService.java:27`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu thẻ gửi xe; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/CardService.java`, bắt đầu tại dòng **27**.

**Bảng/entity liên quan:** `cards`

**Cột/thuộc tính liên quan:** `cards.card_code`, `cards.card_id`, `cards.created_at`, `cards.expired_at`, `cards.issued_at`, `cards.status`, `cards.updated_at`, `cards.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
.orElseGet(() -> cardRepository.save(buildNewCard(user, null)));
```

### `src/main/java/com/swp/parking/service/CardService.java:35`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu thẻ gửi xe; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/CardService.java`, bắt đầu tại dòng **35**.

**Bảng/entity liên quan:** `cards`

**Cột/thuộc tính liên quan:** `cards.card_code`, `cards.card_id`, `cards.created_at`, `cards.expired_at`, `cards.issued_at`, `cards.status`, `cards.updated_at`, `cards.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
cardRepository.save(card);
```

### `src/main/java/com/swp/parking/service/CardService.java:67`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi thẻ gửi xe theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/CardService.java`, bắt đầu tại dòng **67**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return userRepository.findById(userId)
```

### `src/main/java/com/swp/parking/service/FeeSubscriptionService.java:66`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký gói phí theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`, bắt đầu tại dòng **66**.

**Bảng/entity liên quan:** `vehicles`

**Cột/thuộc tính liên quan:** `vehicles.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
```

### `src/main/java/com/swp/parking/service/FeeSubscriptionService.java:78`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký gói phí theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`, bắt đầu tại dòng **78**.

**Bảng/entity liên quan:** `fee_package`

**Cột/thuộc tính liên quan:** `fee_package.fee_package_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
FeePackage feePackage = feePackageRepository.findById(request.getFeePackageId())
```

### `src/main/java/com/swp/parking/service/FeeSubscriptionService.java:94`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu đăng ký gói phí; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`, bắt đầu tại dòng **94**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscription = feeSubscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/FeeSubscriptionService.java:109`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký gói phí theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`, bắt đầu tại dòng **109**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.fee_subscription_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
FeeSubscription subscription = feeSubscriptionRepository.findById(subscriptionId)
```

### `src/main/java/com/swp/parking/service/FeeSubscriptionService.java:121`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu đăng ký gói phí; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`, bắt đầu tại dòng **121**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
feeSubscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/FeeSubscriptionService.java:141`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký gói phí theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`, bắt đầu tại dòng **141**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.fee_subscription_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
FeeSubscription subscription = feeSubscriptionRepository.findById(subscriptionId)
```

### `src/main/java/com/swp/parking/service/FeeSubscriptionService.java:153`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu đăng ký gói phí; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/FeeSubscriptionService.java`, bắt đầu tại dòng **153**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
feeSubscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/MomoQrService.java:89`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/MomoQrService.java`, bắt đầu tại dòng **89**.

**Bảng/entity liên quan:** `momo_order`

**Cột/thuộc tính liên quan:** `momo_order.amount`, `momo_order.created_at`, `momo_order.description`, `momo_order.expired_at`, `momo_order.invoice_id`, `momo_order.momo_account`, `momo_order.momo_name`, `momo_order.notes`, `momo_order.order_id`, `momo_order.paid_at`, `momo_order.payment_status`, `momo_order.qr_code_data`, `momo_order.qr_code_image_url`, `momo_order.subscription_id`, `momo_order.transaction_id`, `momo_order.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
MomoOrder saved = momoOrderRepository.save(order);
```

### `src/main/java/com/swp/parking/service/MomoQrService.java:99`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/MomoQrService.java`, bắt đầu tại dòng **99**.

**Bảng/entity liên quan:** `momo_order`

**Cột/thuộc tính liên quan:** `momo_order.id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return momoOrderRepository.findById(orderId)
```

### `src/main/java/com/swp/parking/service/MomoQrService.java:124`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/MomoQrService.java`, bắt đầu tại dòng **124**.

**Bảng/entity liên quan:** `momo_order`

**Cột/thuộc tính liên quan:** `momo_order.amount`, `momo_order.created_at`, `momo_order.description`, `momo_order.expired_at`, `momo_order.invoice_id`, `momo_order.momo_account`, `momo_order.momo_name`, `momo_order.notes`, `momo_order.order_id`, `momo_order.paid_at`, `momo_order.payment_status`, `momo_order.qr_code_data`, `momo_order.qr_code_image_url`, `momo_order.subscription_id`, `momo_order.transaction_id`, `momo_order.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return momoOrderRepository.save(order);
```

### `src/main/java/com/swp/parking/service/MomoQrService.java:137`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/MomoQrService.java`, bắt đầu tại dòng **137**.

**Bảng/entity liên quan:** `momo_order`

**Cột/thuộc tính liên quan:** `momo_order.amount`, `momo_order.created_at`, `momo_order.description`, `momo_order.expired_at`, `momo_order.invoice_id`, `momo_order.momo_account`, `momo_order.momo_name`, `momo_order.notes`, `momo_order.order_id`, `momo_order.paid_at`, `momo_order.payment_status`, `momo_order.qr_code_data`, `momo_order.qr_code_image_url`, `momo_order.subscription_id`, `momo_order.transaction_id`, `momo_order.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return momoOrderRepository.save(order);
```

### `src/main/java/com/swp/parking/service/NotificationService.java:68`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu thông báo; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/NotificationService.java`, bắt đầu tại dòng **68**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.category`, `notifications.content`, `notifications.created_at`, `notifications.created_by`, `notifications.id`, `notifications.is_active`, `notifications.priority`, `notifications.published_at`, `notifications.recipient_target`, `notifications.recipient_user_id`, `notifications.scheduled_at`, `notifications.status`, `notifications.summary`, `notifications.title`, `notifications.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
notificationRepository.save(notification);
```

### `src/main/java/com/swp/parking/service/NotificationService.java:73`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi thông báo theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/NotificationService.java`, bắt đầu tại dòng **73**.

**Bảng/entity liên quan:** `notifications`

**Cột/thuộc tính liên quan:** `notifications.id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
Notification notification = notificationRepository.findById(id)
```

### `src/main/java/com/swp/parking/service/NotificationService.java:93`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu thông báo; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/NotificationService.java`, bắt đầu tại dòng **93**.

**Bảng/entity liên quan:** `device_tokens`

**Cột/thuộc tính liên quan:** `device_tokens.created_at`, `device_tokens.id`, `device_tokens.platform`, `device_tokens.token`, `device_tokens.updated_at`, `device_tokens.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
deviceTokenRepository.save(existing);
```

### `src/main/java/com/swp/parking/service/NotificationService.java:102`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu thông báo; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/NotificationService.java`, bắt đầu tại dòng **102**.

**Bảng/entity liên quan:** `device_tokens`

**Cột/thuộc tính liên quan:** `device_tokens.created_at`, `device_tokens.id`, `device_tokens.platform`, `device_tokens.token`, `device_tokens.updated_at`, `device_tokens.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
deviceTokenRepository.save(newToken);
```

### `src/main/java/com/swp/parking/service/ParkingSlotService.java:25`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy toàn bộ ô đỗ xe từ database.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingSlotService.java`, bắt đầu tại dòng **25**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.created_at`, `parking_slots.floor`, `parking_slots.id`, `parking_slots.slot_number`, `parking_slots.status`, `parking_slots.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return parkingSlotRepository.findAll().stream()
```

### `src/main/java/com/swp/parking/service/ParkingSlotService.java:32`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi ô đỗ xe theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingSlotService.java`, bắt đầu tại dòng **32**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
ParkingSlot slot = parkingSlotRepository.findById(id)
```

### `src/main/java/com/swp/parking/service/ParkingSlotService.java:48`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu ô đỗ xe; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingSlotService.java`, bắt đầu tại dòng **48**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.created_at`, `parking_slots.floor`, `parking_slots.id`, `parking_slots.slot_number`, `parking_slots.status`, `parking_slots.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return mapToResponse(parkingSlotRepository.save(slot));
```

### `src/main/java/com/swp/parking/service/ParkingSlotService.java:52`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi ô đỗ xe theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingSlotService.java`, bắt đầu tại dòng **52**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
ParkingSlot slot = parkingSlotRepository.findById(id)
```

### `src/main/java/com/swp/parking/service/ParkingSlotService.java:59`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu ô đỗ xe; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingSlotService.java`, bắt đầu tại dòng **59**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.created_at`, `parking_slots.floor`, `parking_slots.id`, `parking_slots.slot_number`, `parking_slots.status`, `parking_slots.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return mapToResponse(parkingSlotRepository.save(slot));
```

### `src/main/java/com/swp/parking/service/ParkingSlotService.java:63`

**Loại query:** Thao tác kiểm tra tồn tại `EXISTS` kế thừa từ `JpaRepository`.

**Mục đích:** Kiểm tra bản ghi ô đỗ xe có tồn tại theo khóa chính hay không.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingSlotService.java`, bắt đầu tại dòng **63**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
if (!parkingSlotRepository.existsById(id)) {
```

### `src/main/java/com/swp/parking/service/ParkingSlotService.java:66`

**Loại query:** Thao tác `DELETE` kế thừa từ `JpaRepository`.

**Mục đích:** Xóa ô đỗ xe theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/ParkingSlotService.java`, bắt đầu tại dòng **66**.

**Bảng/entity liên quan:** `parking_slots`

**Cột/thuộc tính liên quan:** `parking_slots.id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
parkingSlotRepository.deleteById(id);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:105`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **105**.

**Bảng/entity liên quan:** `vehicles`

**Cột/thuộc tính liên quan:** `vehicles.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:113`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **113**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.save(pending);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:125`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **125**.

**Bảng/entity liên quan:** `fee_package`

**Cột/thuộc tính liên quan:** `fee_package.fee_package_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
FeePackage feePackage = packageRepository.findById(request.getPlanId())
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:146`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **146**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscription = subscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:157`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **157**.

**Bảng/entity liên quan:** `fee_subscription_invoice`

**Cột/thuộc tính liên quan:** `fee_subscription_invoice.amount`, `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`, `fee_subscription_invoice.invoice_id`, `fee_subscription_invoice.message`, `fee_subscription_invoice.momo_order_id`, `fee_subscription_invoice.momo_request_id`, `fee_subscription_invoice.momo_trans_id`, `fee_subscription_invoice.status`, `fee_subscription_invoice.type`, `fee_subscription_invoice.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
invoice = invoiceRepository.save(invoice);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:169`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **169**.

**Bảng/entity liên quan:** `fee_subscription_invoice`

**Cột/thuộc tính liên quan:** `fee_subscription_invoice.amount`, `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`, `fee_subscription_invoice.invoice_id`, `fee_subscription_invoice.message`, `fee_subscription_invoice.momo_order_id`, `fee_subscription_invoice.momo_request_id`, `fee_subscription_invoice.momo_trans_id`, `fee_subscription_invoice.status`, `fee_subscription_invoice.type`, `fee_subscription_invoice.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
invoiceRepository.save(invoice);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:200`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **200**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.fee_subscription_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
FeeSubscription subscription = subscriptionRepository.findById(subscriptionId)
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:225`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **225**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:230`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **230**.

**Bảng/entity liên quan:** `fee_subscription_invoice`

**Cột/thuộc tính liên quan:** `fee_subscription_invoice.amount`, `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`, `fee_subscription_invoice.invoice_id`, `fee_subscription_invoice.message`, `fee_subscription_invoice.momo_order_id`, `fee_subscription_invoice.momo_request_id`, `fee_subscription_invoice.momo_trans_id`, `fee_subscription_invoice.status`, `fee_subscription_invoice.type`, `fee_subscription_invoice.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
invoiceRepository.save(invoice);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:238`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **238**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:242`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **242**.

**Bảng/entity liên quan:** `fee_subscription_invoice`

**Cột/thuộc tính liên quan:** `fee_subscription_invoice.amount`, `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`, `fee_subscription_invoice.invoice_id`, `fee_subscription_invoice.message`, `fee_subscription_invoice.momo_order_id`, `fee_subscription_invoice.momo_request_id`, `fee_subscription_invoice.momo_trans_id`, `fee_subscription_invoice.status`, `fee_subscription_invoice.type`, `fee_subscription_invoice.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
invoiceRepository.save(invoice);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:259`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **259**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.fee_subscription_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
FeeSubscription subscription = subscriptionRepository.findById(subscriptionId)
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:273`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **273**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:289`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **289**.

**Bảng/entity liên quan:** `fee_subscription_invoice`

**Cột/thuộc tính liên quan:** `fee_subscription_invoice.amount`, `fee_subscription_invoice.created_at`, `fee_subscription_invoice.fee_subscription_id`, `fee_subscription_invoice.invoice_id`, `fee_subscription_invoice.message`, `fee_subscription_invoice.momo_order_id`, `fee_subscription_invoice.momo_request_id`, `fee_subscription_invoice.momo_trans_id`, `fee_subscription_invoice.status`, `fee_subscription_invoice.type`, `fee_subscription_invoice.updated_at`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
invoiceRepository.save(inv);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:307`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **307**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.fee_subscription_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
FeeSubscription subscription = subscriptionRepository.findById(subscriptionId)
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:323`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **323**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.save(subscription);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:335`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi dữ liệu nghiệp vụ theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **335**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.fee_subscription_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.findById(subscriptionId).ifPresent(sub -> {
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:338`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **338**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.save(sub);
```

### `src/main/java/com/swp/parking/service/SubscriptionService.java:362`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu dữ liệu nghiệp vụ; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/SubscriptionService.java`, bắt đầu tại dòng **362**.

**Bảng/entity liên quan:** `fee_subscription`

**Cột/thuộc tính liên quan:** `fee_subscription.amount_to_pay`, `fee_subscription.created_at`, `fee_subscription.end_date`, `fee_subscription.fee_package_id`, `fee_subscription.fee_subscription_id`, `fee_subscription.is_auto_renew`, `fee_subscription.momo_partner_token`, `fee_subscription.price_history_id`, `fee_subscription.start_date`, `fee_subscription.status`, `fee_subscription.updated_at`, `fee_subscription.vehicle_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
subscriptionRepository.save(active);
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:128`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký phương tiện theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **128**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
User user = userRepository.findById(userId)
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:130`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký phương tiện theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **130**.

**Bảng/entity liên quan:** `vehicle_types`

**Cột/thuộc tính liên quan:** `vehicle_types.vehicle_type_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:234`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu đăng ký phương tiện; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **234**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.brand`, `vehicle_registrations.cccd_back_image`, `vehicle_registrations.cccd_front_image`, `vehicle_registrations.color`, `vehicle_registrations.contact_phone`, `vehicle_registrations.created_at`, `vehicle_registrations.ekyc_cccd_expiry_date`, `vehicle_registrations.ekyc_cccd_id`, `vehicle_registrations.ekyc_cccd_issue_date`, `vehicle_registrations.ekyc_confidence_score`, `vehicle_registrations.ekyc_date_of_birth`, `vehicle_registrations.ekyc_document_type`, `vehicle_registrations.ekyc_face_match_score`, `vehicle_registrations.ekyc_full_name`, `vehicle_registrations.ekyc_gender`, `vehicle_registrations.ekyc_is_fake`, `vehicle_registrations.ekyc_is_valid`, `vehicle_registrations.ekyc_issuing_authority`, `vehicle_registrations.ekyc_license_class`, `vehicle_registrations.ekyc_license_expiry`, `vehicle_registrations.ekyc_license_issue_date`, `vehicle_registrations.ekyc_license_number`, `vehicle_registrations.ekyc_nationality`, `vehicle_registrations.ekyc_place_of_origin`, `vehicle_registrations.ekyc_place_of_residence`, `vehicle_registrations.license_image`, `vehicle_registrations.license_plate`, `vehicle_registrations.plate_image`, `vehicle_registrations.registration_id`, `vehicle_registrations.registration_source`, `vehicle_registrations.reject_reason`, `vehicle_registrations.requested_fee_package_id`, `vehicle_registrations.reviewed_at`, `vehicle_registrations.reviewed_by`, `vehicle_registrations.status`, `vehicle_registrations.updated_at`, `vehicle_registrations.user_id`, `vehicle_registrations.vehicle_document_image`, `vehicle_registrations.vehicle_id`, `vehicle_registrations.vehicle_type_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return toResponse(registrationRepository.save(registration));
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:336`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký phương tiện theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **336**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.registration_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
VehicleRegistration reg = registrationRepository.findById(registrationId)
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:359`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký phương tiện theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **359**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.registration_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
VehicleRegistration reg = registrationRepository.findById(registrationId)
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:366`

**Loại query:** Thao tác đọc `SELECT` kế thừa từ `JpaRepository`.

**Mục đích:** Lấy một bản ghi đăng ký phương tiện theo khóa chính.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **366**.

**Bảng/entity liên quan:** `users`

**Cột/thuộc tính liên quan:** `users.user_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
User admin = userRepository.findById(adminUserId)
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:386`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu đăng ký phương tiện; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **386**.

**Bảng/entity liên quan:** `vehicles`

**Cột/thuộc tính liên quan:** `vehicles.brand`, `vehicles.color`, `vehicles.created_at`, `vehicles.customer_id`, `vehicles.license_plate`, `vehicles.updated_at`, `vehicles.vehicle_id`, `vehicles.vehicle_type_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
.orElseGet(() -> vehicleRepository.save(Vehicle.builder()
```

### `src/main/java/com/swp/parking/service/VehicleRegistrationService.java:402`

**Loại query:** Thao tác ghi `INSERT/UPDATE` kế thừa từ `JpaRepository`.

**Mục đích:** Lưu đăng ký phương tiện; Hibernate quyết định `INSERT` hoặc `UPDATE` theo trạng thái entity.

**Vị trí trong source:** `src/main/java/com/swp/parking/service/VehicleRegistrationService.java`, bắt đầu tại dòng **402**.

**Bảng/entity liên quan:** `vehicle_registrations`

**Cột/thuộc tính liên quan:** `vehicle_registrations.brand`, `vehicle_registrations.cccd_back_image`, `vehicle_registrations.cccd_front_image`, `vehicle_registrations.color`, `vehicle_registrations.contact_phone`, `vehicle_registrations.created_at`, `vehicle_registrations.ekyc_cccd_expiry_date`, `vehicle_registrations.ekyc_cccd_id`, `vehicle_registrations.ekyc_cccd_issue_date`, `vehicle_registrations.ekyc_confidence_score`, `vehicle_registrations.ekyc_date_of_birth`, `vehicle_registrations.ekyc_document_type`, `vehicle_registrations.ekyc_face_match_score`, `vehicle_registrations.ekyc_full_name`, `vehicle_registrations.ekyc_gender`, `vehicle_registrations.ekyc_is_fake`, `vehicle_registrations.ekyc_is_valid`, `vehicle_registrations.ekyc_issuing_authority`, `vehicle_registrations.ekyc_license_class`, `vehicle_registrations.ekyc_license_expiry`, `vehicle_registrations.ekyc_license_issue_date`, `vehicle_registrations.ekyc_license_number`, `vehicle_registrations.ekyc_nationality`, `vehicle_registrations.ekyc_place_of_origin`, `vehicle_registrations.ekyc_place_of_residence`, `vehicle_registrations.license_image`, `vehicle_registrations.license_plate`, `vehicle_registrations.plate_image`, `vehicle_registrations.registration_id`, `vehicle_registrations.registration_source`, `vehicle_registrations.reject_reason`, `vehicle_registrations.requested_fee_package_id`, `vehicle_registrations.reviewed_at`, `vehicle_registrations.reviewed_by`, `vehicle_registrations.status`, `vehicle_registrations.updated_at`, `vehicle_registrations.user_id`, `vehicle_registrations.vehicle_document_image`, `vehicle_registrations.vehicle_id`, `vehicle_registrations.vehicle_type_id`

**Ghi chú:**

Thao tác CRUD được kế thừa từ JpaRepository; SQL thực tế do Hibernate sinh.

```java
return toResponse(registrationRepository.save(reg));
```

