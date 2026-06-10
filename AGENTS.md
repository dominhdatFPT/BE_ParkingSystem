# AGENTS.md — Smart Parking Backend

Java 17 Spring Boot 3.3.5 monolithic REST API. Package root: `com.swp.parking`. Entry point: `ParkingApplication.java`.

## Build & Run
- **Build:** `mvn clean package -DskipTests`
- **Run:** `mvn spring-boot:run` (default port 8080)
- **Test:** `mvn test` (only `ParkingApplicationTests` exists — just a context load)
- **Docker:** `docker build -t parking .` (Dockerfile uses Java 21 for build, but app targets Java 17)
- **PowerShell helper:** `scripts/run-supabase.ps1` sets Supabase env vars then runs `mvn spring-boot:run`

## Architecture (Actual Code)
The docs in `/docs/` describe an older entity structure. The actual code uses these layers:
- `controller/` — REST endpoints under `/api/v1`
- `service/` — `@Service @RequiredArgsConstructor @Transactional`
- `repository/` — Spring Data JPA interfaces
- `model/` — JPA entities (uses `@Data @Builder`, not `@Getter @Setter`)
- `dto/request/` + `dto/response/` — `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- `config/` — `SecurityConfig`, `JwtConfig`, `FirebaseConfig`
- `exception/` — `AppException` (custom runtime) + `GlobalExceptionHandler`
- `model/enums/` — `UserRole`, `ParkingSlotStatus`, `BookingStatus`

## API Conventions
- Base prefix: `/api/v1`
- Auth: `/api/v1/auth/**` (public)
- All other endpoints require JWT (enforced in `SecurityConfig`)
- JWT is passed as `Authorization: Bearer <token>`
- Controllers return `ResponseEntity<T>` directly (not wrapped in a generic `ApiResponse` — the docs mention an `ApiResponse` wrapper but it does not exist in the current code)
- CORS allows `localhost:5173` (Vite frontend), `localhost:8080`, and `127.0.0.1` variants

## Security
- JWT secret and expiration are in `application.yml` (hardcoded — not env-driven)
- `JwtConfig` holds both token generation logic and the `JwtAuthenticationFilter` (same file)
- `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` returns `Long` (userId)
- `PasswordEncoder` bean is `BCryptPasswordEncoder`

## Database
- PostgreSQL via Supabase (external cloud instance)
- `ddl-auto: none` — no schema generation; rely on existing Supabase schema
- `show-sql: true` in dev
- `spring.jpa.open-in-view: false`

## Firebase
- `FirebaseConfig` initializes `FirebaseApp` from `firebase-service-account.json` on classpath
- If Firebase bean is called multiple times, it returns the existing instance
- Used for push notifications (not currently active in auth flow — see TODO comments in `AuthService`)

## Important Gotchas
- **Entity layer uses `@Data` on JPA entities** (which includes `@EqualsAndHashCode` — could cause issues with lazy collections). This is the current team convention.
- **User role is `@Transient`** — it is not stored in the `users` table. The role is resolved dynamically at login via `UserRepository.findActiveEmployeeRoleByUserId()` (native SQL query on `employees` table). If not an active employee, role defaults to `USER`.
- **Password column is `password_hash`** in the DB but mapped as `password` in the entity.
- **No integration tests** — only `ParkingApplicationTests#contextLoads` exists. Testing requires a live Supabase connection or testcontainer setup.
- **Hardcoded credentials in `application.yml`** — Supabase DB URL/password are committed. Do not change this unless the team explicitly requests it.
- **`.env` file exists but is ignored** — it contains `PORT=5000` and `API_PREFIX=/api`, but these are NOT used by Spring Boot. The actual config is in `application.yml`.

## Adding a New Feature
1. Create/update entity in `model/` if needed
2. Add repository in `repository/`
3. Add DTOs in `dto/request/` and `dto/response/`
4. Add service in `service/` with `@Transactional`
5. Add controller in `controller/` with `@RestController @RequestMapping("/api/v1/...")`
6. Update `SecurityConfig` if new endpoints need special access rules
