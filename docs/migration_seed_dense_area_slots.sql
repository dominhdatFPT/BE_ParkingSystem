-- Migration: Seed real database slots for every visible area A/B/C/D.
-- This keeps Welcome and Staff Operations backed by the same Supabase data.
-- Safe to run multiple times.

WITH desired AS (
  SELECT
    pf.parking_id,
    fl.floor_id,
    fl.floor_number,
    area.area_code,
    slot_no.n,
    CASE
      WHEN slot_no.n = 1 THEN 'OCCUPIED'
      WHEN slot_no.n = 2 THEN 'RESERVED'
      WHEN slot_no.n = 6 THEN 'MAINTENANCE'
      ELSE 'AVAILABLE'
    END AS status
  FROM parking_facilities pf
  JOIN parking_floors fl ON fl.parking_id = pf.parking_id
  CROSS JOIN (VALUES ('A'), ('B'), ('C'), ('D')) AS area(area_code)
  CROSS JOIN generate_series(1, 6) AS slot_no(n)
),
prepared AS (
  SELECT
    parking_id,
    floor_id,
    floor_number,
    area_code,
    CASE parking_id
      WHEN 1 THEN 'LK'
      WHEN 2 THEN 'BH'
      ELSE 'TCM'
    END || '-' || area_code || floor_number || LPAD(n::text, 2, '0') AS slot_number,
    status
  FROM desired d
  WHERE NOT EXISTS (
    SELECT 1 FROM parking_slots ps
    WHERE ps.parking_id = d.parking_id
      AND ps.floor_id = d.floor_id
      AND COALESCE(
        SUBSTRING(ps.slot_number FROM '^[A-Za-z]+-([A-Za-z])'),
        SUBSTRING(ps.slot_number FROM '^([A-Za-z])'),
        'A'
      ) = d.area_code
  )
)
INSERT INTO parking_slots (slot_number, floor, parking_id, floor_id, status, created_at, updated_at)
SELECT slot_number, floor_number, parking_id, floor_id, status, NOW(), NOW()
FROM prepared
ORDER BY parking_id, floor_number, area_code, slot_number;

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

DELETE FROM parking_area_counts;

WITH slot_areas AS (
  SELECT
    CASE pf.parking_id
      WHEN 1 THEN 'LK'
      WHEN 2 THEN 'BH'
      ELSE 'TCM'
    END AS building_code,
    pf.parking_name AS building_name,
    ps.floor AS floor_number,
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
    END AS vehicle_type,
    ps.status
  FROM parking_slots ps
  JOIN parking_facilities pf ON pf.parking_id = ps.parking_id
),
summary AS (
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
    COUNT(*) FILTER (WHERE status IN ('OCCUPIED', 'RESERVED'))::int AS current_vehicle_count
  FROM slot_areas
  GROUP BY building_code, building_name, floor_number, area_code
)
INSERT INTO parking_area_counts (
  building_code,
  building_name,
  floor_number,
  area_code,
  vehicle_type,
  capacity,
  current_vehicle_count,
  created_at,
  updated_at
)
SELECT building_code, building_name, floor_number, area_code, vehicle_type, capacity, current_vehicle_count, NOW(), NOW()
FROM summary
WHERE capacity > 0
ORDER BY building_code, floor_number, area_code;
