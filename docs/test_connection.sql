-- Test Connection Query - Check if we can read data
SELECT 
    COUNT(*) as total_slots,
    COUNT(CASE WHEN status = 'AVAILABLE' THEN 1 END) as available,
    COUNT(CASE WHEN status = 'OCCUPIED' THEN 1 END) as occupied,
    COUNT(CASE WHEN status = 'RESERVED' THEN 1 END) as reserved,
    COUNT(CASE WHEN status = 'MAINTENANCE' THEN 1 END) as maintenance
FROM parking_slots;

-- Check floors
SELECT floor_id, floor_name, floor_number, parking_id 
FROM parking_floors 
ORDER BY floor_number;

-- Check existing slots by floor
SELECT floor, COUNT(*) as slot_count, status 
FROM parking_slots 
GROUP BY floor, status 
ORDER BY floor;
