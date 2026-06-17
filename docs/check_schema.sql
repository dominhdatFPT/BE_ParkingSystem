-- Check current database schema structure
-- Run this to see what tables and columns exist

-- 1. List all tables
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- 2. Check parking_facilities structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'parking_facilities'
ORDER BY ordinal_position;

-- 3. Check if parking_zones table exists
SELECT EXISTS (
    SELECT 1 FROM information_schema.tables 
    WHERE table_name = 'parking_zones'
);

-- 4. Check if parking_floors table exists
SELECT EXISTS (
    SELECT 1 FROM information_schema.tables 
    WHERE table_name = 'parking_floors'
);

-- 5. Check parking_slots structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'parking_slots'
ORDER BY ordinal_position;

-- 6. Check all tables structure
SELECT t.table_name, c.column_name, c.data_type, c.is_nullable
FROM information_schema.tables t
JOIN information_schema.columns c ON t.table_name = c.table_name
WHERE t.table_schema = 'public' 
AND t.table_name LIKE 'parking%'
ORDER BY t.table_name, c.ordinal_position;
