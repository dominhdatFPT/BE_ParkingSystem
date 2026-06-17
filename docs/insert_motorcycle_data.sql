-- Insert/replace sample motorcycle parking data for floor B2.
--
-- Current Supabase schema used by the running API stores the map directly in
-- parking_slots: slot_number, floor, status. It does not have parking_zones or
-- parking_slots.zone_id yet, so this script intentionally does not depend on
-- zone tables.
--
-- Effect:
-- - Convert B01..B10 into M-B201..M-B210.
-- - Convert a few car/demo C slots (C05..C10) into M-B211..M-B216.
-- - The UI/test script can then update M-B201..M-B216 without returning blank.

BEGIN;

-- Remove previous motorcycle demo rows if they were inserted manually before.
DELETE FROM parking_slots
WHERE slot_number IN (
  'M-B201', 'M-B202', 'M-B203', 'M-B204',
  'M-B205', 'M-B206', 'M-B207', 'M-B208',
  'M-B209', 'M-B210', 'M-B211', 'M-B212',
  'M-B213', 'M-B214', 'M-B215', 'M-B216'
);

-- Reuse existing rows first so we replace some old car/generic demo data
-- instead of only appending more rows.
WITH source_slots AS (
  SELECT
    id,
    ROW_NUMBER() OVER (
      ORDER BY
        CASE WHEN slot_number LIKE 'B%' THEN 0 ELSE 1 END,
        slot_number
    ) AS rn
  FROM parking_slots
  WHERE slot_number IN (
    'B01', 'B02', 'B03', 'B04', 'B05',
    'B06', 'B07', 'B08', 'B09', 'B10',
    'C05', 'C06', 'C07', 'C08', 'C09', 'C10'
  )
),
target_slots AS (
  SELECT *
  FROM (VALUES
    (1, 'M-B201', 'AVAILABLE'),
    (2, 'M-B202', 'OCCUPIED'),
    (3, 'M-B203', 'AVAILABLE'),
    (4, 'M-B204', 'RESERVED'),
    (5, 'M-B205', 'AVAILABLE'),
    (6, 'M-B206', 'OCCUPIED'),
    (7, 'M-B207', 'AVAILABLE'),
    (8, 'M-B208', 'OCCUPIED'),
    (9, 'M-B209', 'AVAILABLE'),
    (10, 'M-B210', 'AVAILABLE'),
    (11, 'M-B211', 'OCCUPIED'),
    (12, 'M-B212', 'MAINTENANCE'),
    (13, 'M-B213', 'MAINTENANCE'),
    (14, 'M-B214', 'RESERVED'),
    (15, 'M-B215', 'OCCUPIED'),
    (16, 'M-B216', 'MAINTENANCE')
  ) AS v(rn, slot_number, status)
)
UPDATE parking_slots ps
SET
  slot_number = t.slot_number,
  floor = 2,
  status = t.status,
  updated_at = NOW()
FROM source_slots s
JOIN target_slots t ON t.rn = s.rn
WHERE ps.id = s.id;

-- If the DB did not have enough B/C source rows, append the missing
-- motorcycle rows.
WITH target_slots AS (
  SELECT *
  FROM (VALUES
    ('M-B201', 'AVAILABLE'),
    ('M-B202', 'OCCUPIED'),
    ('M-B203', 'AVAILABLE'),
    ('M-B204', 'RESERVED'),
    ('M-B205', 'AVAILABLE'),
    ('M-B206', 'OCCUPIED'),
    ('M-B207', 'AVAILABLE'),
    ('M-B208', 'OCCUPIED'),
    ('M-B209', 'AVAILABLE'),
    ('M-B210', 'AVAILABLE'),
    ('M-B211', 'OCCUPIED'),
    ('M-B212', 'MAINTENANCE'),
    ('M-B213', 'MAINTENANCE'),
    ('M-B214', 'RESERVED'),
    ('M-B215', 'OCCUPIED'),
    ('M-B216', 'MAINTENANCE')
  ) AS v(slot_number, status)
)
INSERT INTO parking_slots (slot_number, floor, status, created_at, updated_at)
SELECT t.slot_number, 2, t.status, NOW(), NOW()
FROM target_slots t
WHERE NOT EXISTS (
  SELECT 1
  FROM parking_slots ps
  WHERE ps.slot_number = t.slot_number
);

COMMIT;

-- Verify motorcycle data.
SELECT
  floor,
  COUNT(*) AS total,
  COUNT(CASE WHEN status = 'AVAILABLE' THEN 1 END) AS available,
  COUNT(CASE WHEN status = 'OCCUPIED' THEN 1 END) AS occupied,
  COUNT(CASE WHEN status = 'RESERVED' THEN 1 END) AS reserved,
  COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) AS maintenance
FROM parking_slots
WHERE slot_number LIKE 'M-B2%'
GROUP BY floor
ORDER BY floor;
