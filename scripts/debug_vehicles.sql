-- ============================================================
-- DIAGNOSTIC: Tìm nguyên nhân xe không hiện ra
-- Chạy từng block một, đọc kết quả rồi tiếp tục
-- ============================================================

-- BLOCK 1: Xem tất cả users đang có trong DB
-- → Kiểm tra email của user "Nguyễn Minh Hoàng"
SELECT user_id, email, full_name, created_at
FROM public.users
ORDER BY created_at DESC;

-- ============================================================

-- BLOCK 2: Xem vehicle_types đang có (kiểm tra type_id thực tế)
-- → FE hardcode MOTORBIKE=1, CAR=2 — nếu DB khác thì sẽ lọc sai
SELECT vehicle_type_id, type_name, type_code
FROM public.vehicle_types
ORDER BY vehicle_type_id;

-- ============================================================

-- BLOCK 3: Xem customers đang có
SELECT c.customer_id, c.user_id, u.email, u.full_name
FROM public.customers c
JOIN public.users u ON u.user_id = c.user_id
ORDER BY c.created_at DESC;

-- ============================================================

-- BLOCK 4: Xem tất cả vehicles đang có
SELECT
    v.vehicle_id,
    v.license_plate,
    v.brand,
    vt.type_name,
    vt.vehicle_type_id,
    u.email,
    u.full_name
FROM public.vehicles v
JOIN public.customers c  ON c.customer_id     = v.customer_id
JOIN public.users u      ON u.user_id         = c.user_id
JOIN public.vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
ORDER BY v.created_at DESC;
