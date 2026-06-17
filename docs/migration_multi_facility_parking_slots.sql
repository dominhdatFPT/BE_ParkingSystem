-- Migration: Link parking_slots to facilities/floors and seed Bien Hoa slots.
-- Safe to run multiple times.

ALTER TABLE parking_slots
  ADD COLUMN IF NOT EXISTS parking_id bigint,
  ADD COLUMN IF NOT EXISTS floor_id bigint;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_slots_parking'
  ) THEN
    ALTER TABLE parking_slots
      ADD CONSTRAINT fk_parking_slots_parking
      FOREIGN KEY (parking_id) REFERENCES parking_facilities(parking_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_slots_floor'
  ) THEN
    ALTER TABLE parking_slots
      ADD CONSTRAINT fk_parking_slots_floor
      FOREIGN KEY (floor_id) REFERENCES parking_floors(floor_id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_parking_slots_parking_id ON parking_slots(parking_id);
CREATE INDEX IF NOT EXISTS idx_parking_slots_floor_id ON parking_slots(floor_id);

INSERT INTO parking_floors (
  parking_id,
  floor_name,
  floor_number,
  max_capacity,
  current_vehicle_count,
  status,
  created_at,
  updated_at
)
SELECT
  1,
  U&'T\1EA7ng 3 - Long Kh\00E1nh',
  3,
  100,
  0,
  'ACTIVE',
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM parking_floors WHERE parking_id = 1 AND floor_number = 3
);

UPDATE parking_slots ps
SET parking_id = pf.parking_id,
    floor_id = pf.floor_id,
    updated_at = NOW()
FROM parking_floors pf
WHERE ps.parking_id IS NULL
  AND ps.floor_id IS NULL
  AND pf.parking_id = 1
  AND pf.floor_number = ps.floor;

WITH bien_hoa_floor_1 AS (
  SELECT floor_id FROM parking_floors WHERE parking_id = 2 AND floor_number = 1 LIMIT 1
),
bien_hoa_floor_2 AS (
  SELECT floor_id FROM parking_floors WHERE parking_id = 2 AND floor_number = 2 LIMIT 1
),
seed_slots(slot_number, floor, parking_id, floor_id, status) AS (
  SELECT slot_number, 1, 2, (SELECT floor_id FROM bien_hoa_floor_1), status
  FROM (VALUES
    ('BH-A01', 'AVAILABLE'),
    ('BH-A02', 'AVAILABLE'),
    ('BH-A03', 'OCCUPIED'),
    ('BH-A04', 'RESERVED'),
    ('BH-A05', 'AVAILABLE'),
    ('BH-A06', 'AVAILABLE'),
    ('BH-A07', 'AVAILABLE'),
    ('BH-A08', 'MAINTENANCE')
  ) AS v(slot_number, status)
  UNION ALL
  SELECT slot_number, 2, 2, (SELECT floor_id FROM bien_hoa_floor_2), status
  FROM (VALUES
    ('BH-B01', 'AVAILABLE'),
    ('BH-B02', 'OCCUPIED'),
    ('BH-B03', 'AVAILABLE'),
    ('BH-B04', 'AVAILABLE'),
    ('BH-B05', 'RESERVED'),
    ('BH-B06', 'AVAILABLE'),
    ('BH-B07', 'MAINTENANCE'),
    ('BH-B08', 'AVAILABLE')
  ) AS v(slot_number, status)
)
INSERT INTO parking_slots (slot_number, floor, parking_id, floor_id, status, created_at, updated_at)
SELECT slot_number, floor, parking_id, floor_id, status, NOW(), NOW()
FROM seed_slots s
WHERE s.floor_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM parking_slots ps
    WHERE ps.parking_id = s.parking_id
      AND ps.floor_id = s.floor_id
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
