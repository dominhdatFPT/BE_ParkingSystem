# Database Usage Audit

Audit date: 2026-06-17

This audit compares the current Spring Boot backend source with the 29 public tables in Supabase.

## Tables Used Directly By Current Backend Code

These tables are referenced by JPA entities, repositories, services, controllers, or native SQL in `src/main/java`.

| Table | Used by |
| --- | --- |
| `users` | Auth, user APIs, vehicle registration owner/reviewer |
| `employees` | Login role resolution through `UserRepository.findActiveEmployeeRoleByUserId` |
| `customers` | Vehicle approval creates vehicles for a customer |
| `vehicles` | Approved vehicle registrations, active parking orders |
| `vehicle_types` | Vehicle registration, booking, parking zones |
| `vehicle_registrations` | Vehicle/eKYC registration workflow |
| `cards` | Card API/service |
| `bookings` | Booking API/service, staff booking review, dashboard summaries |
| `parking_facilities` | Parking structure, operations dashboard, welcome/area summary |
| `parking_floors` | Parking structure, operations dashboard, booking, welcome/area summary |
| `parking_zones` | Parking structure, booking zone capacity |
| `parking_slots` | Parking structure, operations dashboard, booking, welcome/area summary |
| `parking_orders` | Active vehicles, check-in/check-out metrics, operations dashboard |
| `buildings` | Native SQL in `ParkingAreaSummaryService` |

Total directly used tables: 14 / 29.

## Tables Not Referenced By Current Backend Code

These tables exist in Supabase but are not currently referenced by runtime backend code.

| Table | Current rows | Notes |
| --- | ---: | --- |
| `audit_logs` | 2 | No current audit log service/repository |
| `companies` | 3 | Only a DB dependency for `buildings.company_id`; no backend code reads it |
| `floor_vehicle_types` | 5 | No current entity/repository/service |
| `gates` | 4 | Only a DB dependency for `parking_orders.entry_gate_id/exit_gate_id`; backend maps gate ids as `Long` |
| `packages` | 2 | No current package registration API yet |
| `parking_area_counts` | 32 | Legacy/summary table; current code aggregates from `parking_slots` |
| `parking_images` | 3 | No current parking image API/service |
| `parking_lots` | 0 | No current entity/repository/service |
| `parking_statistics` | 2 | No current entity/repository/service |
| `parking_ticket` | 0 | No current entity/repository/service |
| `password_reset_tokens` | 7 | No current forgot-password flow in backend code |
| `payments` | 1 | No current payment entity/repository/service; booking stores payment fields directly |
| `pricing_rules` | 5 | No current pricing rule API/service |
| `reservations` | 1 | Only a DB dependency for `parking_orders.reservation_id`; no current backend code reads it |
| `vehicle_packages` | 2 | No current vehicle package API/service |

Total not referenced by current backend code: 15 / 29.

## Important Warning

Do not drop tables directly in the live Supabase database unless the team agrees these features are out of scope.
Some unused tables are likely planned for later features, especially:

- `packages`
- `vehicle_packages`
- `payments`
- `pricing_rules`
- `gates`
- `reservations`
- `audit_logs`

The safest cleanup for the current vehicle registration workflow is to reset data, not drop schema.

