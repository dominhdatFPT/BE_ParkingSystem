-- Sample parking data for the current running schema.
--
-- The live Supabase schema currently stores the map directly in parking_slots
-- and does not have parking_zones/zone_id yet. This script keeps a few car
-- slots on B3 and replaces the rest of the demo B/C rows with motorcycle
-- slots M-B201..M-B216 on B2.

BEGIN;

-- Vehicle types for screens that read this reference table.
INSERT INTO vehicle_types (type_name, type_code, created_at, updated_at)
SELECT 'O to', 'CAR', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM vehicle_types WHERE type_code = 'CAR'
);

INSERT INTO vehicle_types (type_name, type_code, created_at, updated_at)
SELECT 'Xe may', 'BIKE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM vehicle_types WHERE type_code = 'BIKE'
);

-- Remove old target demo rows if they already exist.
DELETE FROM parking_slots
WHERE slot_number IN (
  'M-C301', 'M-C302', 'M-C303', 'M-C304',
  'M-C305', 'M-C306', 'M-C307', 'M-C308',
  'M-C309', 'M-C310', 'M-C311', 'M-C312',
  'M-C313', 'M-C314', 'M-C315', 'M-C316',
  'M-C317', 'M-C318', 'M-C319', 'M-C320',
  'M-C321', 'M-C322', 'M-C323', 'M-C324',
  'M-C325', 'M-C326', 'M-C327', 'M-C328',
  'M-C329', 'M-C330', 'M-C331', 'M-C332',
  'M-C333', 'M-C334', 'M-C335', 'M-C336',
  'M-C337', 'M-C338', 'M-C339', 'M-C340',
  'M-C341', 'M-C342', 'M-C343', 'M-C344',
  'M-C345', 'M-C346', 'M-C347', 'M-C348',
  'M-B201', 'M-B202', 'M-B203', 'M-B204',
  'M-B205', 'M-B206', 'M-B207', 'M-B208',
  'M-B209', 'M-B210', 'M-B211', 'M-B212',
  'M-B213', 'M-B214', 'M-B215', 'M-B216'
);

-- Keep/recreate a small car sample on B3.
INSERT INTO parking_slots (slot_number, floor, status, created_at, updated_at)
SELECT s.slot_number, 3, 'AVAILABLE', NOW(), NOW()
FROM (VALUES
  ('M-C301'), ('M-C302'), ('M-C303'), ('M-C304')
) AS s(slot_number)
WHERE NOT EXISTS (
  SELECT 1 FROM parking_slots ps WHERE ps.slot_number = s.slot_number
);

-- Replace old B01..B10 and a few C slots with motorcycle slots.
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

-- Insert any missing motorcycle slots if the old source rows were not present.
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
  SELECT 1 FROM parking_slots ps WHERE ps.slot_number = t.slot_number
);

COMMIT;

-- Verify by floor.
SELECT
  floor,
  COUNT(*) AS total_slots,
  COUNT(CASE WHEN status = 'AVAILABLE' THEN 1 END) AS available,
  COUNT(CASE WHEN status = 'OCCUPIED' THEN 1 END) AS occupied,
  COUNT(CASE WHEN status = 'RESERVED' THEN 1 END) AS reserved,
  COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) AS maintenance
FROM parking_slots
WHERE floor IN (2, 3)
GROUP BY floor
ORDER BY floor DESC;
