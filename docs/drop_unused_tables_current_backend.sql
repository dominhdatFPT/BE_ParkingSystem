-- Destructive cleanup script.
-- Scope: keep only the vehicle registration/eKYC review flow.
--
-- Tables intentionally kept:
-- users, employees, vehicle_types, customers, vehicles, vehicle_registrations
--
-- Everything else is dropped because the user wants to rebuild other flows later.

BEGIN;

DROP TABLE IF EXISTS
    audit_logs,
    companies,
    bookings,
    buildings,
    cards,
    floor_vehicle_types,
    gates,
    packages,
    parking_facilities,
    parking_floors,
    parking_area_counts,
    parking_images,
    parking_lots,
    parking_orders,
    parking_slots,
    parking_statistics,
    parking_ticket,
    parking_zones,
    password_reset_tokens,
    payments,
    pricing_rules,
    reservations,
    vehicle_packages
CASCADE;

COMMIT;
