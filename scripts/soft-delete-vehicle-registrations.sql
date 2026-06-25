-- Soft Delete Vehicle Registrations
-- Thêm cột is_deleted và deleted_at cho bảng vehicle_registrations

ALTER TABLE vehicle_registrations
    ADD COLUMN IF NOT EXISTS is_deleted boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS deleted_at timestamp without time zone;

-- Đánh dấu các bản ghi hiện tại là chưa xóa (mặc định DEFAULT false đã xử lý)
-- Cập nhật lại để đảm bảo consistency
UPDATE vehicle_registrations SET is_deleted = false WHERE is_deleted IS NULL;

-- Index hỗ trợ truy vấn soft delete
CREATE INDEX IF NOT EXISTS idx_vehicle_registrations_is_deleted
    ON vehicle_registrations (is_deleted);

CREATE INDEX IF NOT EXISTS idx_vehicle_registrations_license_plate_active
    ON vehicle_registrations (license_plate)
    WHERE is_deleted = false;
