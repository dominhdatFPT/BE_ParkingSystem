-- Test real-time update - watch the UI change without refreshing.
-- Run docs/insert_motorcycle_data.sql first so M-B201..M-B216 exist.

-- 1. Change some AVAILABLE motorcycle slots to OCCUPIED.
UPDATE parking_slots
SET status = 'OCCUPIED', updated_at = NOW()
WHERE slot_number IN ('M-B203', 'M-B205', 'M-B207', 'M-B209')
  AND floor = 2
  AND status = 'AVAILABLE';

-- 2. Free up some OCCUPIED motorcycle slots.
UPDATE parking_slots
SET status = 'AVAILABLE', updated_at = NOW()
WHERE slot_number IN ('M-B202', 'M-B206')
  AND floor = 2
  AND status = 'OCCUPIED';

-- 3. View current motorcycle state.
SELECT
  floor,
  COUNT(*) AS total_slots,
  COUNT(CASE WHEN status = 'AVAILABLE' THEN 1 END) AS available,
  COUNT(CASE WHEN status = 'OCCUPIED' THEN 1 END) AS occupied,
  COUNT(CASE WHEN status = 'RESERVED' THEN 1 END) AS reserved,
  COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) AS maintenance
FROM parking_slots
WHERE slot_number LIKE 'M-B2%'
GROUP BY floor
ORDER BY floor;
