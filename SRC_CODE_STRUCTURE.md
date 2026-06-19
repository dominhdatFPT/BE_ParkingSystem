# Cấu trúc source code

Tài liệu này mô tả toàn bộ cấu trúc thư mục `src` hiện tại của backend Smart Parking.

## Tổng quan

- Source chính nằm trong `src/main/java/com/java/parking`.
- Cấu hình ứng dụng nằm trong `src/main/resources`.
- Test hiện tại nằm trong `src/test/java/com/swp/parking`.

## Cây thư mục `src`

```text
src
+---main
|   +---java
|   |   \---com
|   |       \---java
|   |           \---parking
|   |               |   ParkingApplication.java
|   |               |
|   |               +---config
|   |               |       EkycProperties.java
|   |               |       FirebaseConfig.java
|   |               |       JwtConfig.java
|   |               |       SecurityConfig.java
|   |               |
|   |               +---controller
|   |               |       AuthController.java
|   |               |       BookingController.java
|   |               |       CustomerParkingOrderController.java
|   |               |       FeeSubscriptionController.java
|   |               |       ParkingAreaSummaryController.java
|   |               |       ParkingEntryController.java
|   |               |       ParkingSlotController.java
|   |               |       ParkingStructureController.java
|   |               |       StaffBookingController.java
|   |               |       StaffOperationsController.java
|   |               |       UserController.java
|   |               |       VehicleRegistrationController.java
|   |               |
|   |               +---dto
|   |               |   +---ekyc
|   |               |   |       EkycCccdResult.java
|   |               |   |       EkycLicenseResult.java
|   |               |   |       EkycValidationResult.java
|   |               |   |
|   |               |   +---request
|   |               |   |       AdminReviewRequest.java
|   |               |   |       BookingPaymentRequest.java
|   |               |   |       BookingRequest.java
|   |               |   |       CreateSubscriptionRequest.java
|   |               |   |       GoogleLoginRequest.java
|   |               |   |       LoginRequest.java
|   |               |   |       ParkingEntryCheckRequest.java
|   |               |   |       ParkingEntryConfirmRequest.java
|   |               |   |       ParkingSlotRequest.java
|   |               |   |       RegisterRequest.java
|   |               |   |       StaffBookingDecisionRequest.java
|   |               |   |       UserBookingRequest.java
|   |               |   |       VehicleRegistrationRequest.java
|   |               |   |
|   |               |   \---response
|   |               |           ActiveParkingOrderResponse.java
|   |               |           ApiResponse.java
|   |               |           AuthResponse.java
|   |               |           BookingResponse.java
|   |               |           CreateSubscriptionResponse.java
|   |               |           FeePackageResponse.java
|   |               |           MyVehicleResponse.java
|   |               |           OperationsDashboardResponse.java
|   |               |           ParkingAreaOptionsResponse.java
|   |               |           ParkingAreaSummaryResponse.java
|   |               |           ParkingEntryResponse.java
|   |               |           ParkingFloorResponse.java
|   |               |           ParkingOperationsResponse.java
|   |               |           ParkingSlotResponse.java
|   |               |           UserResponse.java
|   |               |           VehicleRegistrationResponse.java
|   |               |
|   |               +---exception
|   |               |       AppException.java
|   |               |       FeePackageNotFoundException.java
|   |               |       GlobalExceptionHandler.java
|   |               |       InvalidSubscriptionStatusException.java
|   |               |       SubscriptionNotFoundException.java
|   |               |       VehicleAlreadyHasActiveSubscriptionException.java
|   |               |       VehicleNotOwnedByUserException.java
|   |               |
|   |               +---model
|   |               |   |   Booking.java
|   |               |   |   Building.java
|   |               |   |   Card.java
|   |               |   |   Customer.java
|   |               |   |   FeePackage.java
|   |               |   |   FeePackagePriceHistory.java
|   |               |   |   FeeSubscription.java
|   |               |   |   ParkingFacility.java
|   |               |   |   ParkingFloor.java
|   |               |   |   ParkingOrder.java
|   |               |   |   ParkingSlot.java
|   |               |   |   ParkingZone.java
|   |               |   |   User.java
|   |               |   |   Vehicle.java
|   |               |   |   VehicleRegistration.java
|   |               |   |   VehicleType.java
|   |               |   |
|   |               |   \---enums
|   |               |           BookingStatus.java
|   |               |           CardStatus.java
|   |               |           ParkingSlotStatus.java
|   |               |           PaymentStatus.java
|   |               |           SubscriptionStatus.java
|   |               |           UserRole.java
|   |               |
|   |               +---repository
|   |               |       BookingRepository.java
|   |               |       CardRepository.java
|   |               |       CustomerRepository.java
|   |               |       FeePackagePriceHistoryRepository.java
|   |               |       FeePackageRepository.java
|   |               |       FeeSubscriptionRepository.java
|   |               |       ParkingFacilityRepository.java
|   |               |       ParkingFloorRepository.java
|   |               |       ParkingOrderRepository.java
|   |               |       ParkingSlotRepository.java
|   |               |       ParkingZoneRepository.java
|   |               |       UserRepository.java
|   |               |       VehicleRegistrationRepository.java
|   |               |       VehicleRepository.java
|   |               |       VehicleTypeRepository.java
|   |               |
|   |               \---service
|   |                       AuthService.java
|   |                       BookingService.java
|   |                       CardService.java
|   |                       CustomerParkingOrderService.java
|   |                       CustomerParkingOrderServiceImpl.java
|   |                       EkycService.java
|   |                       FeePackageService.java
|   |                       FeeSubscriptionService.java
|   |                       OperationsDashboardService.java
|   |                       ParkingAreaSummaryService.java
|   |                       ParkingEntryService.java
|   |                       ParkingSlotService.java
|   |                       ParkingStructureService.java
|   |                       SecurityRoleService.java
|   |                       UserService.java
|   |                       VehicleRegistrationService.java
|
|   \---resources
|           application.yml
|           firebase-service-account.json
|
\---test
    \---java
        \---com
            \---swp
                \---parking
                        ParkingApplicationTests.java
```

## Ý nghĩa các thư mục chính

- `config`: cấu hình bảo mật, JWT, Firebase và eKYC.
- `controller`: các REST controller nhận request từ client.
- `dto`: các object dùng để nhận request, trả response và dữ liệu eKYC.
- `exception`: custom exception và global exception handler.
- `model`: các entity JPA và enum của domain.
- `repository`: các Spring Data JPA repository thao tác database.
- `service`: business logic của hệ thống.
- `resources`: file cấu hình Spring Boot và Firebase service account.
- `test`: test context hiện có của ứng dụng.
