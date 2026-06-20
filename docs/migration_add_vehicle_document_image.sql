ALTER TABLE vehicle_registrations
    ADD COLUMN IF NOT EXISTS vehicle_document_image TEXT;

COMMENT ON COLUMN vehicle_registrations.vehicle_document_image IS
    'Ảnh giấy đăng ký xe do người dùng cung cấp để OCR và nhân viên đối chiếu.';
