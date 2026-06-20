-- Persist all information collected by the AI registration assistant.
-- Safe to run more than once on PostgreSQL/Supabase.
ALTER TABLE vehicle_registrations
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS requested_fee_package_id BIGINT,
    ADD COLUMN IF NOT EXISTS registration_source VARCHAR(20) NOT NULL DEFAULT 'FORM';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_vehicle_registrations_requested_fee_package'
    ) THEN
        ALTER TABLE vehicle_registrations
            ADD CONSTRAINT fk_vehicle_registrations_requested_fee_package
            FOREIGN KEY (requested_fee_package_id)
            REFERENCES fee_package(fee_package_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_vehicle_registrations_requested_fee_package
    ON vehicle_registrations(requested_fee_package_id);
