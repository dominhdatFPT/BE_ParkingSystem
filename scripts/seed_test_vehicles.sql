-- =============================================================================
-- Seed: Thêm xe test để kiểm tra chức năng thanh toán VNPay
-- Chạy trong Supabase SQL Editor
--
-- Đổi biến p_email thành email tài khoản USER bạn đang dùng để test.
-- =============================================================================

DO $$
DECLARE
    p_email         TEXT    := 'nguyenquyen82005@gmail.com';  -- ← đổi thành email của bạn
    v_user_id       BIGINT;
    v_customer_id   BIGINT;
    v_type_moto_id  BIGINT;
    v_type_car_id   BIGINT;
    v_vehicle1_id   BIGINT;
    v_vehicle2_id   BIGINT;
BEGIN

    -- ─── 1. Tìm user ────────────────────────────────────────────────
    SELECT user_id INTO v_user_id
    FROM public.users
    WHERE email = p_email;

    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Không tìm thấy user với email: %', p_email;
    END IF;
    RAISE NOTICE 'User ID = %', v_user_id;

    -- ─── 2. Đảm bảo vehicle_types tồn tại ──────────────────────────
    -- Motorbike (type_code bắt đầu bằng MOTO hoặc MOTORBIKE)
    SELECT vehicle_type_id INTO v_type_moto_id
    FROM public.vehicle_types
    WHERE UPPER(type_code) LIKE '%MOTO%'
       OR UPPER(type_name) LIKE '%XE MÁY%'
       OR UPPER(type_name) LIKE '%MOTOR%'
    LIMIT 1;

    IF v_type_moto_id IS NULL THEN
        INSERT INTO public.vehicle_types (type_name, type_code, created_at, updated_at)
        VALUES ('Xe máy', 'MOTORBIKE', NOW(), NOW())
        RETURNING vehicle_type_id INTO v_type_moto_id;
        RAISE NOTICE 'Đã tạo vehicle_type Xe máy, ID = %', v_type_moto_id;
    ELSE
        RAISE NOTICE 'vehicle_type Xe máy đã tồn tại, ID = %', v_type_moto_id;
    END IF;

    -- Ô tô (type_code bắt đầu bằng CAR)
    SELECT vehicle_type_id INTO v_type_car_id
    FROM public.vehicle_types
    WHERE UPPER(type_code) LIKE '%CAR%'
       OR UPPER(type_name) LIKE '%Ô TÔ%'
       OR UPPER(type_name) LIKE '%OTO%'
    LIMIT 1;

    IF v_type_car_id IS NULL THEN
        INSERT INTO public.vehicle_types (type_name, type_code, created_at, updated_at)
        VALUES ('Ô tô', 'CAR', NOW(), NOW())
        RETURNING vehicle_type_id INTO v_type_car_id;
        RAISE NOTICE 'Đã tạo vehicle_type Ô tô, ID = %', v_type_car_id;
    ELSE
        RAISE NOTICE 'vehicle_type Ô tô đã tồn tại, ID = %', v_type_car_id;
    END IF;

    -- ─── 3. Đảm bảo customer record tồn tại cho user này ────────────
    SELECT customer_id INTO v_customer_id
    FROM public.customers
    WHERE user_id = v_user_id;

    IF v_customer_id IS NULL THEN
        INSERT INTO public.customers (user_id, created_at, updated_at)
        VALUES (v_user_id, NOW(), NOW())
        RETURNING customer_id INTO v_customer_id;
        RAISE NOTICE 'Đã tạo customer record, ID = %', v_customer_id;
    ELSE
        RAISE NOTICE 'Customer đã tồn tại, ID = %', v_customer_id;
    END IF;

    -- ─── 4. Thêm xe máy test ────────────────────────────────────────
    IF NOT EXISTS (
        SELECT 1 FROM public.vehicles
        WHERE customer_id = v_customer_id AND license_plate = '29B1-99999'
    ) THEN
        INSERT INTO public.vehicles
            (customer_id, vehicle_type_id, license_plate, brand, color, created_at, updated_at)
        VALUES
            (v_customer_id, v_type_moto_id, '29B1-99999', 'Honda Wave', 'Đỏ', NOW(), NOW())
        RETURNING vehicle_id INTO v_vehicle1_id;
        RAISE NOTICE 'Đã thêm xe máy: 29B1-99999, vehicle_id = %', v_vehicle1_id;
    ELSE
        RAISE NOTICE 'Xe máy 29B1-99999 đã tồn tại, bỏ qua';
    END IF;

    -- ─── 5. Thêm ô tô test ──────────────────────────────────────────
    IF NOT EXISTS (
        SELECT 1 FROM public.vehicles
        WHERE customer_id = v_customer_id AND license_plate = '51F-88888'
    ) THEN
        INSERT INTO public.vehicles
            (customer_id, vehicle_type_id, license_plate, brand, color, created_at, updated_at)
        VALUES
            (v_customer_id, v_type_car_id, '51F-88888', 'Toyota Vios', 'Trắng', NOW(), NOW())
        RETURNING vehicle_id INTO v_vehicle2_id;
        RAISE NOTICE 'Đã thêm ô tô: 51F-88888, vehicle_id = %', v_vehicle2_id;
    ELSE
        RAISE NOTICE 'Ô tô 51F-88888 đã tồn tại, bỏ qua';
    END IF;

END $$;

-- =============================================================================
-- Kiểm tra kết quả
-- =============================================================================
SELECT
    v.vehicle_id,
    v.license_plate,
    v.brand,
    v.color,
    vt.type_name,
    vt.type_code,
    u.email,
    v.created_at
FROM public.vehicles v
JOIN public.customers c ON c.customer_id = v.customer_id
JOIN public.users     u ON u.user_id     = c.user_id
JOIN public.vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
ORDER BY v.created_at DESC
LIMIT 10;
