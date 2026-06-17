-- Migration Script: Add parking_zones and zone_id to parking_slots.
--
-- The current Supabase schema may not have parking_zones yet. Create it first,
-- then add the nullable parking_slots.zone_id column and foreign key.

-- Step 1: Create parking_zones if it does not exist.
CREATE TABLE IF NOT EXISTS parking_zones (
    zone_id BIGSERIAL PRIMARY KEY,
    parking_id BIGINT NOT NULL REFERENCES parking_facilities(parking_id),
    floor_id BIGINT NOT NULL REFERENCES parking_floors(floor_id),
    vehicle_type_id BIGINT NOT NULL REFERENCES vehicle_types(vehicle_type_id),
    zone_name VARCHAR(255) NOT NULL,
    total_slots INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Step 2: Add zone_id column if needed.
ALTER TABLE parking_slots
ADD COLUMN IF NOT EXISTS zone_id BIGINT;

-- Step 3: Add foreign key constraint if needed.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'parking_slots'
          AND constraint_name = 'fk_parking_slots_zone'
    ) THEN
        ALTER TABLE parking_slots
        ADD CONSTRAINT fk_parking_slots_zone
        FOREIGN KEY (zone_id) REFERENCES parking_zones(zone_id);
    END IF;
END $$;

-- Step 4: Keep zone_id nullable until existing slots are assigned to zones.
-- ALTER TABLE parking_slots ALTER COLUMN zone_id SET NOT NULL;

-- Step 5: Verify the changes.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'parking_slots'
ORDER BY ordinal_position;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'parking_zones';
