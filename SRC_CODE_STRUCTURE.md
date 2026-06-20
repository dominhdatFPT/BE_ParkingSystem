# Cấu trúc source code Backend

> Smart Parking Backend — cập nhật ngày 21/06/2026.

## Tổng quan

- Source backend: `BE/src`.
- Java 17, Spring Boot 3.3.5.
- Package root: `com.swp.parking`.
- Entry point: `ParkingApplication.java`.
- Kiến trúc chính: Controller → Service → Repository/JdbcTemplate → PostgreSQL/Supabase.
- API prefix chính: `/api/v1`; customer API dùng `/api/customer`.
- Xác thực bằng JWT Bearer; principal là `Long userId`.

## Cây thư mục `BE/src`

```text
src/
├── main/
│   ├── java/com/swp/parking/
│   │   ├── ParkingApplication.java
│   │   ├── config/
│   │   │   ├── EkycProperties.java
│   │   │   ├── FirebaseConfig.java
│   │   │   ├── JwtConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── SupportSchemaInitializer.java
│   │   ├── controller/
│   │   │   ├── AdminNotificationController.java
│   │   │   ├── AiChatController.java
│   │   │   ├── AuthController.java
│   │   │   ├── BookingController.java
│   │   │   ├── CustomerNotificationController.java
│   │   │   ├── CustomerParkingOrderController.java
│   │   │   ├── CustomerSupportController.java
│   │   │   ├── FeeSubscriptionController.java
│   │   │   ├── MomoOrderController.java
│   │   │   ├── NotificationController.java
│   │   │   ├── ParkingAreaSummaryController.java
│   │   │   ├── ParkingEntryController.java
│   │   │   ├── ParkingExitController.java
│   │   │   ├── ParkingSlotController.java
│   │   │   ├── ParkingStructureController.java
│   │   │   ├── StaffBookingController.java
│   │   │   ├── StaffOperationsController.java
│   │   │   ├── SubscriptionController.java
│   │   │   ├── SystemDataController.java
│   │   │   ├── UserController.java
│   │   │   ├── VehicleRegistrationController.java
│   │   │   └── VehicleTypeController.java
│   │   ├── dto/
│   │   │   ├── ekyc/
│   │   │   │   ├── EkycCccdResult.java
│   │   │   │   ├── EkycLicenseResult.java
│   │   │   │   └── EkycValidationResult.java
│   │   │   ├── request/
│   │   │   │   ├── AdminNotificationRequest.java
│   │   │   │   ├── AdminReviewRequest.java
│   │   │   │   ├── AiChatRequest.java
│   │   │   │   ├── BookingPaymentRequest.java
│   │   │   │   ├── BookingRequest.java
│   │   │   │   ├── CreateSubscriptionRequest.java
│   │   │   │   ├── GoogleLoginRequest.java
│   │   │   │   ├── IncidentReplyRequest.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── MomoIpnRequest.java
│   │   │   │   ├── ParkingEntryCheckRequest.java
│   │   │   │   ├── ParkingEntryConfirmRequest.java
│   │   │   │   ├── ParkingExitCheckRequest.java
│   │   │   │   ├── ParkingExitConfirmRequest.java
│   │   │   │   ├── ParkingSlotRequest.java
│   │   │   │   ├── RegisterDeviceTokenRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── StaffBookingDecisionRequest.java
│   │   │   │   ├── SubscriptionRegisterRequest.java
│   │   │   │   ├── SupportRequest.java
│   │   │   │   ├── UserBookingRequest.java
│   │   │   │   └── VehicleRegistrationRequest.java
│   │   │   └── response/
│   │   │       ├── ActiveParkingOrderResponse.java
│   │   │       ├── AdminNotificationResponse.java
│   │   │       ├── AiChatResponse.java
│   │   │       ├── ApiResponse.java
│   │   │       ├── AuthResponse.java
│   │   │       ├── BookingResponse.java
│   │   │       ├── CreateSubscriptionResponse.java
│   │   │       ├── CustomerNotificationResponse.java
│   │   │       ├── FeePackageResponse.java
│   │   │       ├── MomoOrderStatusResponse.java
│   │   │       ├── MyVehicleResponse.java
│   │   │       ├── NotificationDetailResponse.java
│   │   │       ├── NotificationListItemResponse.java
│   │   │       ├── OperationsDashboardResponse.java
│   │   │       ├── ParkingAreaOptionsResponse.java
│   │   │       ├── ParkingAreaSummaryResponse.java
│   │   │       ├── ParkingEntryResponse.java
│   │   │       ├── ParkingExitResponse.java
│   │   │       ├── ParkingFloorResponse.java
│   │   │       ├── ParkingOperationsResponse.java
│   │   │       ├── ParkingSlotResponse.java
│   │   │       ├── RegisterSubscriptionResponse.java
│   │   │       ├── SubscriptionInvoiceResponse.java
│   │   │       ├── SubscriptionResponse.java
│   │   │       ├── UserResponse.java
│   │   │       ├── VehicleRegistrationResponse.java
│   │   │       └── VehicleTypeResponse.java
│   │   ├── exception/
│   │   │   ├── AppException.java
│   │   │   ├── FeePackageNotFoundException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── InvalidSubscriptionStatusException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── SubscriptionNotFoundException.java
│   │   │   ├── VehicleAlreadyHasActiveSubscriptionException.java
│   │   │   └── VehicleNotOwnedByUserException.java
│   │   ├── model/
│   │   │   ├── Booking.java
│   │   │   ├── Building.java
│   │   │   ├── Card.java
│   │   │   ├── Customer.java
│   │   │   ├── DeviceToken.java
│   │   │   ├── FeePackage.java
│   │   │   ├── FeePackagePriceHistory.java
│   │   │   ├── FeeSubscription.java
│   │   │   ├── FeeSubscriptionInvoice.java
│   │   │   ├── MomoOrder.java
│   │   │   ├── Notification.java
│   │   │   ├── ParkingFacility.java
│   │   │   ├── ParkingFloor.java
│   │   │   ├── ParkingOrder.java
│   │   │   ├── ParkingSlot.java
│   │   │   ├── ParkingZone.java
│   │   │   ├── User.java
│   │   │   ├── Vehicle.java
│   │   │   ├── VehicleRegistration.java
│   │   │   ├── VehicleType.java
│   │   │   └── enums/
│   │   │       ├── BookingStatus.java
│   │   │       ├── CardStatus.java
│   │   │       ├── InvoiceStatus.java
│   │   │       ├── MomoOrderStatus.java
│   │   │       ├── NotificationCategory.java
│   │   │       ├── NotificationPriority.java
│   │   │       ├── NotificationRecipientTarget.java
│   │   │       ├── NotificationStatus.java
│   │   │       ├── ParkingSlotStatus.java
│   │   │       ├── PaymentStatus.java
│   │   │       ├── Platform.java
│   │   │       ├── SubscriptionStatus.java
│   │   │       └── UserRole.java
│   │   ├── repository/
│   │   │   ├── BookingRepository.java
│   │   │   ├── CardRepository.java
│   │   │   ├── CustomerRepository.java
│   │   │   ├── DeviceTokenRepository.java
│   │   │   ├── FeePackagePriceHistoryRepository.java
│   │   │   ├── FeePackageRepository.java
│   │   │   ├── FeeSubscriptionInvoiceRepository.java
│   │   │   ├── FeeSubscriptionRepository.java
│   │   │   ├── MomoOrderRepository.java
│   │   │   ├── NotificationRepository.java
│   │   │   ├── ParkingFacilityRepository.java
│   │   │   ├── ParkingFloorRepository.java
│   │   │   ├── ParkingOrderRepository.java
│   │   │   ├── ParkingSlotRepository.java
│   │   │   ├── ParkingZoneRepository.java
│   │   │   ├── UserRepository.java
│   │   │   ├── VehicleRegistrationRepository.java
│   │   │   ├── VehicleRepository.java
│   │   │   └── VehicleTypeRepository.java
│   │   └── service/
│   │       ├── AdminNotificationService.java
│   │       ├── AiChatService.java
│   │       ├── AiRegistrationService.java
│   │       ├── AuthService.java
│   │       ├── BookingService.java
│   │       ├── CardService.java
│   │       ├── CryptoService.java
│   │       ├── CustomerParkingOrderService.java
│   │       ├── CustomerParkingOrderServiceImpl.java
│   │       ├── EkycService.java
│   │       ├── FeePackageService.java
│   │       ├── FeeSubscriptionService.java
│   │       ├── GeminiService.java
│   │       ├── MomoQrService.java
│   │       ├── NotificationService.java
│   │       ├── OperationsDashboardService.java
│   │       ├── ParkingAreaSummaryService.java
│   │       ├── ParkingEntryService.java
│   │       ├── ParkingExitService.java
│   │       ├── ParkingSlotService.java
│   │       ├── ParkingStructureService.java
│   │       ├── SecurityRoleService.java
│   │       ├── SubscriptionService.java
│   │       ├── SystemDataService.java
│   │       ├── UserService.java
│   │       └── VehicleRegistrationService.java
│   └── resources/
│       ├── application.yml
│       └── firebase-service-account.json
└── test/
    └── java/com/swp/parking/
        ├── ParkingApplicationTests.java
        └── service/
```

## Ý nghĩa các package

- `config`: cấu hình JWT, Spring Security, Firebase, eKYC và schema hỗ trợ.
- `controller`: định nghĩa REST API, kiểm tra request và lấy user hiện tại từ JWT.
- `dto/ekyc`: object kết quả OCR CCCD và bằng lái.
- `dto/request`: request body kèm validation Jakarta.
- `dto/response`: response trả về client.
- `exception`: custom exception và xử lý lỗi tập trung.
- `model`: JPA entity ánh xạ bảng PostgreSQL.
- `model/enums`: enum trạng thái và phân loại nghiệp vụ.
- `repository`: Spring Data JPA truy cập database.
- `service`: business logic và transaction.
- `resources`: cấu hình Spring Boot và Firebase credential.
- `test`: context test và unit test service hiện có.

## Nhóm nghiệp vụ chính

| Nhóm | Thành phần chính |
|---|---|
| Xác thực/JWT | `AuthController`, `AuthService`, `JwtConfig`, `SecurityConfig` |
| User và phân quyền | `UserController`, `UserService`, `SecurityRoleService` |
| Booking | `BookingController`, `StaffBookingController`, `BookingService` |
| Xe vào/ra | `ParkingEntryController`, `ParkingExitController` và service tương ứng |
| Cấu trúc bãi xe | `ParkingStructureController`, `ParkingSlotController` |
| Đăng ký xe/eKYC | `VehicleRegistrationController`, `VehicleRegistrationService`, `EkycService` |
| Gói gửi xe | `FeeSubscriptionController`, `SubscriptionController` và service tương ứng |
| Thanh toán MoMo | `MomoOrderController`, `MomoQrService`, `MomoOrder` |
| Thông báo | `AdminNotificationController`, `CustomerNotificationController`, `NotificationService` |
| Hỗ trợ/sự cố | `CustomerSupportController`, `SystemDataController`, `SystemDataService` |
| AI chat | `AiChatController`, `AiChatService`, `GeminiService`, `AiRegistrationService` |
| Dashboard staff | `StaffOperationsController`, `OperationsDashboardService` |
| Cấu hình/audit | `SystemDataController`, `SystemDataService` |

## Luồng hỗ trợ và thông báo

```text
POST /api/customer/support
  → CustomerSupportController
  → SystemDataService.createSupportRequest()
  → INSERT parking_incidents với created_by lấy từ JWT

PATCH /api/v1/incidents/{id}/reply
  → SystemDataController
  → SystemDataService.replyIncident()
  → UPDATE parking_incidents
  → NotificationService.createIncidentReplyNotification()
  → INSERT notifications với recipient_user_id của người gửi sự cố

GET /api/customer/notifications
  → CustomerNotificationController
  → NotificationService.getCustomerNotifications(userId)
  → Chỉ trả broadcast và thông báo riêng đúng user hiện tại
```

Phân quyền:

- `/api/customer/support/**`: `ROLE_USER`.
- `/api/v1/incidents/**`: `ROLE_ADMIN` hoặc `ROLE_STAFF`.
- `/api/customer/notifications`: yêu cầu JWT hợp lệ.

## Cấu hình và database

- Database PostgreSQL/Supabase.
- `spring.jpa.hibernate.ddl-auto: none`.
- `spring.jpa.open-in-view: false`.
- `SupportSchemaInitializer` bảo đảm tồn tại:
  - `parking_incidents.reply_title`.
  - `notifications.recipient_user_id`.
  - Index `idx_notifications_recipient_user_id`.
- `SystemDataService` dùng `JdbcTemplate` cho incident, audit log và system configuration.
- Các domain còn lại chủ yếu dùng Spring Data JPA repository.

## Build và test backend

```powershell
cd D:\LEARN_FPT\SWP\src\BE

# Compile/package
mvn clean package -DskipTests

# Test
mvn test

# Chạy backend
mvn spring-boot:run

# Build Docker image
docker build -t parking .
```

## Lưu ý hiện tại

- Backend mặc định chạy port `8080`.
- Docker Compose đang đặt `EKYC_PROVIDER=mock`.
- Mock CCCD và bằng lái được sinh theo `userId`; biển số và giấy đăng ký xe giữ logic mock cũ để đối chiếu khớp nhau.
- Dữ liệu sự cố, phản hồi và notification được lưu thật trong Supabase.
- Notification phản hồi sự cố được giới hạn theo `recipient_user_id`.
- API đánh dấu notification đã đọc hiện chưa lưu trạng thái đọc riêng từng user; trong `NotificationService` vẫn có TODO cho phần này.
- Số lượng test tự động còn ít, nên cần kiểm thử API với database thật sau thay đổi nghiệp vụ.
