📁 Cấu trúc file & Logic BE - Smart Parking System

Package gốc:com.swp.parking


🗂️ Cấu trúc thư mục
src/main/java/com/swp/parking/
│
├── SmartParkingApplication.java       # Entry point
│
├── config/
│   ├── SecurityConfig.java            # Cấu hình Spring Security + JWT filter
│   └── FirebaseConfig.java            # Khởi tạo Firebase Admin SDK
│
├── controller/
│   ├── AuthController.java            # POST /api/auth/login, /google-login
│   ├── ParkingInfoController.java     # GET /api/parking/me/**
│   ├── UserController.java            # (chờ implement)
│   └── BookingController.java         # (chờ implement)
│
├── service/
│   ├── AuthService.java               # Logic đăng nhập email + Google
│   ├── VehicleInfoService.java        # Logic Flow 3: xem xe đang gửi
│   ├── ParkingLocationService.java    # Logic Flow 4: xem vị trí + thời gian
│   ├── EstimatedFeeService.java       # Logic Flow 5: tính phí tạm tính
│   └── BookingService.java            # (chờ implement)
│
├── repository/
│   ├── UserRepository.java            # findByEmail
│   ├── CustomerRepository.java        # findByUser_UserId
│   ├── EmployeeRepository.java        # findByUserId
│   ├── ParkingOrderRepository.java    # findAllActiveOrdersByUserId
│   ├── PricingRuleRepository.java     # findByParkingId + vehicleTypeId
│   ├── BookingRepository.java         # (chờ implement)
│   └── VehicleRepository.java         # (chờ implement)
│
├── entity/
│   ├── User.java                      # Bảng users
│   ├── Customer.java                  # Bảng customers
│   ├── Employee.java                  # Bảng employees
│   ├── Vehicle.java                   # Bảng vehicles
│   ├── VehicleType.java               # Bảng vehicle_types
│   ├── ParkingFacility.java           # Bảng parking_facilities
│   ├── ParkingFloor.java              # Bảng parking_floors
│   ├── ParkingOrder.java              # Bảng parking_orders
│   └── PricingRule.java               # Bảng pricing_rules
│
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java          # { email, password }
│   │   └── FirebaseLoginRequest.java  # { idToken }
│   └── response/
│       ├── ApiResponse.java           # Wrapper chung { success, message, data }
│       ├── LoginResponse.java         # { token, userId, role, ... }
│       ├── VehicleInfoResponse.java   # Flow 3
│       ├── ParkingLocationResponse.java # Flow 4
│       └── EstimatedFeeResponse.java  # Flow 5
│
├── security/
│   ├── JwtTokenProvider.java          # Tạo + đọc JWT token
│   └── JwtAuthenticationFilter.java   # Filter kiểm tra token mỗi request
│
└── exception/
├── ResourceNotFoundException.java  # Custom 404
├── ErrorResponse.java              # { status, message, timestamp }
└── GlobalExceptionHandler.java     # Xử lý tất cả exception → response

🔄 Luồng xử lý Request (Request Flow)
[Client gửi Request]
↓
[JwtAuthenticationFilter]
→ Đọc header: Authorization: Bearer <token>
→ Validate token
→ Set userId + role vào SecurityContext
↓
[SecurityConfig - Phân quyền]
→ /api/auth/** → permit all
→ /api/admin/** → cần role ADMIN
→ /api/employee/** → cần role ADMIN/SECURITY/CASHIER
→ /api/parking/me/** → cần authenticated
↓
[Controller]
→ Nhận request
→ Lấy userId từ SecurityContext
→ Gọi Service
→ Bọc kết quả trong ApiResponse
→ Trả về ResponseEntity
↓
[Service]
→ Xử lý business logic
→ Gọi Repository
→ Map Entity → DTO
→ Trả về DTO
↓
[Repository]
→ Query database bằng JPQL
→ Trả về Entity
↓
[Database - Supabase PostgreSQL]

📦 Layer chi tiết
1. Entity Layer
   Vị trí:  com.swp.parking.entity
   Nhiệm vụ: Ánh xạ trực tiếp với bảng trong DB
   Quy tắc:

@Entity @Table(name="tên_bảng")
@Column(name="tên_cột")cho mỗi field
@ManyToOne @JoinColumncho quan hệ FK
Dùng @Getter @Setter(KHÔNG dùng @Data)
Long= BIGINT, BigDecimal= NUMERIC, LocalDateTime= TIMESTAMP

Quan hệ giữa các Entity:
User (1) ──── Customer (N) ──── Vehicle (N) ──── ParkingOrder (N)
User (1) ──── Employee (N)                              │
├── ParkingFacility
├── ParkingFloor
└── parking_status
PricingRule (N) ──── ParkingFacility (theo parking_id)

2. Repository Layer
   Vị trí:  com.swp.parking.repository
   Nhiệm vụ: Truy vấn database, KHÔNG chứa business logic
   Quy tắc:

ExtendJpaRepository<Entity, Long>
Custom query dùng @QueryJPQL
Tên method theo convention Spring Data JPA

Ví dụ quan trọng:
Java// Lấy tất cả đơn đang gửi của user
@Query("""
SELECT o FROM ParkingOrder o
WHERE o.vehicle.customer.user.userId = :userId
AND o.parkingStatus IN ('ACTIVE', 'CHECKED_IN')
ORDER BY o.entryTime DESC
""")
List<ParkingOrder> findAllActiveOrdersByUserId(@Param("userId") Long userId);

3. DTO Layer
   Vị trí:  com.swp.parking.dto
   Nhiệm vụ: Định nghĩa format data gửi/nhận giữa client và server
   Quy tắc:

request/→ data nhận từ client
response/→ data from your customer
Dùng@Data @Builder @NoArgsConstructor @AllArgsConstructor
KHÔNG chứa logic

ApiResponse wrapper:
Java// Mọi response đều được bọc trong:
{
"success": true/false,
"message": null/"lỗi gì đó",
"data": { ... }
}

4. Service Layer
   Vị trí:  com.swp.parking.service
   Nhiệm vụ: Xử lý toàn bộ business logic
   Quy tắc:

@Service @RequiredArgsConstructor @Slf4j
Inject Repository qua constructor
Map Entity → DTO tại đây
Throw exception nếu không tìm thấy data
Trả về DTO, KHÔNG trả về Entity

Ví dụ logic tính phí:
1. Lấy danh sách đơn active của user
2. Với mỗi đơn:
    - Tính durationMinutes = now - entryTime
    - Xác định dayType (WEEKDAY/WEEKEND)
    - Tìm pricing_rule phù hợp (theo bãi + loại xe + khung giờ + ngày)
    - fee = rule.price × durationMinutes
3. Trả về list EstimatedFeeResponse

5. Controller Layer
   Vị trí:  com.swp.parking.controller
   Nhiệm vụ: Nhận request, gọi service, trả response
   Quy tắc:

@RestController @RequestMapping @RequiredArgsConstructor @Slf4j
Lấy userId từ SecurityContext:

Java  Long userId = (Long) SecurityContextHolder
.getContext().getAuthentication().getPrincipal();

Bọc kết quả trongApiResponse.success(data)
KHÔNG chứa business logic


6. Security Layer
   Vị trí:  com.swp.parking.security
   Nhiệm vụ: Xác thực JWT token
   JwtTokenProvider:

generateToken(userId, role)→ tạo JWT
getUserIdFromToken(token)→ lấy userId
getRoleFromToken(token)→ lấy role
validateToken(token)→ kiểm tra hợp lệ

JwtAuthenticationFilter:

Chạy trước mỗi request
Đọc headerAuthorization: Bearer <token>
Validate → set vào SecurityContext


7. Exception Layer
   Vị trí:  com.swp.parking.exception
   Nhiệm vụ: Xử lý lỗi tập trung
   ExceptionHTTP StatusKhi nàoResourceNotFoundException404Không tìm thấy dataBadCredentialsException400Sai mật khẩuRuntimeException403Tài khoản bị khóaException500Lỗi hệ thống

⚙️ Config Layer
Vị trí: com.swp.parking.config
FileNhiệm vụSecurityConfig.javaCấu hình Spring Security, phân quyền API, BCryptFirebaseConfig.javaKhởi tạo Firebase Admin SDK từ JSON file

📌 Nguyên tắc chung khi thêm chức năng mới
Bước 1: Tạo/cập nhật Entity (nếu cần bảng mới)
Bước 2: Tạo Repository với custom query
Bước 3: Tạo DTO (request + response)
Bước 4: Tạo Service với business logic
Bước 5: Tạo/cập nhật Controller với endpoint mới
Bước 6: Cập nhật SecurityConfig nếu cần phân quyền mới