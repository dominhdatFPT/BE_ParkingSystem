-- Migration: Add the third parking facility used by the frontend option TCM.
-- Safe to run multiple times.

WITH company_seed AS (
  INSERT INTO companies (company_name, tax_code, phone, email, address, status, created_at, updated_at)
  SELECT
    U&'C\00F4ng ty V\1EADn h\00E0nh B\00E3i xe To\00E0n C\1EA9m M\1EF9',
    '0908070605',
    '02513873333',
    'cammy@sps.vn',
    U&'Huy\1EC7n C\1EA9m M\1EF9, \0110\1ED3ng Nai',
    'ACTIVE',
    NOW(),
    NOW()
  WHERE NOT EXISTS (SELECT 1 FROM companies WHERE email = 'cammy@sps.vn')
  RETURNING company_id
),
company_ref AS (
  SELECT company_id FROM company_seed
  UNION ALL
  SELECT company_id FROM companies WHERE email = 'cammy@sps.vn'
  LIMIT 1
),
building_seed AS (
  INSERT INTO buildings (company_id, building_name, address, phone, email, status, created_at, updated_at)
  SELECT
    company_id,
    U&'T\00F2a nh\00E0 To\00E0n C\1EA9m M\1EF9, \0110\1ED3ng Nai',
    U&'S\1ED1 22 H\00F9ng V\01B0\01A1ng, th\1ECB tr\1EA5n Long Giao, huy\1EC7n C\1EA9m M\1EF9, \0110\1ED3ng Nai',
    '02513873333',
    'cammy@sps.vn',
    'ACTIVE',
    NOW(),
    NOW()
  FROM company_ref
  WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE email = 'cammy@sps.vn')
  RETURNING building_id
),
building_ref AS (
  SELECT building_id FROM building_seed
  UNION ALL
  SELECT building_id FROM buildings WHERE email = 'cammy@sps.vn'
  LIMIT 1
),
parking_seed AS (
  INSERT INTO parking_facilities (
    building_id,
    parking_name,
    description,
    max_capacity,
    current_vehicle_count,
    operating_start,
    operating_end,
    status,
    created_at,
    updated_at
  )
  SELECT
    building_id,
    U&'B\00E3i xe Th\00F4ng minh To\00E0n C\1EA9m M\1EF9',
    U&'B\00E3i \0111\1ED7 xe t\1EF1 \0111\1ED9ng t\00F2a To\00E0n C\1EA9m M\1EF9, \0110\1ED3ng Nai',
    240,
    0,
    '05:00:00'::time,
    '23:30:00'::time,
    'ACTIVE',
    NOW(),
    NOW()
  FROM building_ref
  WHERE NOT EXISTS (
    SELECT 1 FROM parking_facilities
    WHERE parking_name = U&'B\00E3i xe Th\00F4ng minh To\00E0n C\1EA9m M\1EF9'
  )
  RETURNING parking_id
),
parking_ref AS (
  SELECT parking_id FROM parking_seed
  UNION ALL
  SELECT parking_id FROM parking_facilities
  WHERE parking_name = U&'B\00E3i xe Th\00F4ng minh To\00E0n C\1EA9m M\1EF9'
  LIMIT 1
)
INSERT INTO parking_floors (parking_id, floor_name, floor_number, max_capacity, current_vehicle_count, status, created_at, updated_at)
SELECT
  p.parking_id,
  CASE f.floor_number
    WHEN 1 THEN U&'T\1EA7ng 1 - To\00E0n C\1EA9m M\1EF9'
    WHEN 2 THEN U&'T\1EA7ng 2 - To\00E0n C\1EA9m M\1EF9'
    WHEN 3 THEN U&'T\1EA7ng 3 - To\00E0n C\1EA9m M\1EF9'
  END,
  f.floor_number,
  80,
  0,
  'ACTIVE',
  NOW(),
  NOW()
FROM parking_ref p
CROSS JOIN (VALUES (1), (2), (3)) AS f(floor_number)
WHERE NOT EXISTS (
  SELECT 1 FROM parking_floors existing
  WHERE existing.parking_id = p.parking_id
    AND existing.floor_number = f.floor_number
);

WITH parking_ref AS (
  SELECT parking_id FROM parking_facilities
  WHERE parking_name = U&'B\00E3i xe Th\00F4ng minh To\00E0n C\1EA9m M\1EF9'
  LIMIT 1
),
seed_slots(slot_number, floor_number, status) AS (
  VALUES
    ('TCM-A01', 1, 'AVAILABLE'),
    ('TCM-A02', 1, 'AVAILABLE'),
    ('TCM-A03', 1, 'OCCUPIED'),
    ('TCM-A04', 1, 'RESERVED'),
    ('TCM-A05', 1, 'AVAILABLE'),
    ('TCM-A06', 1, 'AVAILABLE'),
    ('TCM-A07', 1, 'MAINTENANCE'),
    ('TCM-A08', 1, 'AVAILABLE'),
    ('TCM-B01', 2, 'AVAILABLE'),
    ('TCM-B02', 2, 'OCCUPIED'),
    ('TCM-B03', 2, 'AVAILABLE'),
    ('TCM-B04', 2, 'AVAILABLE'),
    ('TCM-B05', 2, 'RESERVED'),
    ('TCM-B06', 2, 'AVAILABLE'),
    ('TCM-B07', 2, 'AVAILABLE'),
    ('TCM-B08', 2, 'MAINTENANCE'),
    ('TCM-C01', 3, 'AVAILABLE'),
    ('TCM-C02', 3, 'AVAILABLE'),
    ('TCM-C03', 3, 'AVAILABLE'),
    ('TCM-C04', 3, 'OCCUPIED'),
    ('TCM-C05', 3, 'AVAILABLE'),
    ('TCM-C06', 3, 'RESERVED'),
    ('TCM-C07', 3, 'AVAILABLE'),
    ('TCM-C08', 3, 'AVAILABLE')
)
INSERT INTO parking_slots (slot_number, floor, parking_id, floor_id, status, created_at, updated_at)
SELECT s.slot_number, s.floor_number, p.parking_id, fl.floor_id, s.status, NOW(), NOW()
FROM seed_slots s
JOIN parking_ref p ON true
JOIN parking_floors fl ON fl.parking_id = p.parking_id AND fl.floor_number = s.floor_number
WHERE NOT EXISTS (
  SELECT 1 FROM parking_slots ps
  WHERE ps.parking_id = p.parking_id
    AND ps.floor_id = fl.floor_id
    AND ps.slot_number = s.slot_number
);

UPDATE parking_facilities pf
SET current_vehicle_count = sub.current_vehicle_count,
    max_capacity = GREATEST(COALESCE(pf.max_capacity, 0), sub.total_slots),
    updated_at = NOW()
FROM (
  SELECT parking_id,
         COUNT(*) AS total_slots,
         COUNT(*) FILTER (WHERE status IN ('OCCUPIED', 'RESERVED')) AS current_vehicle_count
  FROM parking_slots
  WHERE parking_id IS NOT NULL
  GROUP BY parking_id
) sub
WHERE pf.parking_id = sub.parking_id;

UPDATE parking_floors fl
SET current_vehicle_count = sub.current_vehicle_count,
    max_capacity = GREATEST(COALESCE(fl.max_capacity, 0), sub.total_slots),
    updated_at = NOW()
FROM (
  SELECT floor_id,
         COUNT(*) AS total_slots,
         COUNT(*) FILTER (WHERE status IN ('OCCUPIED', 'RESERVED')) AS current_vehicle_count
  FROM parking_slots
  WHERE floor_id IS NOT NULL
  GROUP BY floor_id
) sub
WHERE fl.floor_id = sub.floor_id;
