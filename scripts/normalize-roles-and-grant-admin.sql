-- =============================================================================
-- Script: Chuẩn hóa role nhân viên và cấp quyền ADMIN cho admin@smartparking.com
-- Mục tiêu: Chỉ còn 3 role duy nhất ADMIN | STAFF | USER
--           ADMIN và STAFF có quyền sử dụng toàn bộ chức năng quản trị.
-- =============================================================================
-- Chạy script này trong Supabase SQL Editor (hoặc psql) sau khi deploy backend mới.
-- Lưu ý: Nếu bảng employees có thêm cột NOT NULL, hãy bổ sung vào câu INSERT bên dưới.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Bước 1: Chuẩn hóa toàn bộ role nhân viên về ADMIN hoặc STAFF
--         - ADMIN giữ nguyên
--         - SECURITY, CASHIER, MANAGER, SUPERVISOR, ... -> STAFF
-- -----------------------------------------------------------------------------
UPDATE public.employees
SET role       = CASE WHEN UPPER(role) = 'ADMIN' THEN 'ADMIN' ELSE 'STAFF' END,
    updated_at = NOW()
WHERE status = 'ACTIVE';

-- -----------------------------------------------------------------------------
-- Bước 2: Đảm bảo tài khoản admin@smartparking.com có role ADMIN trong bảng employees
-- -----------------------------------------------------------------------------
DO
$$
    DECLARE
        admin_user_id BIGINT;
    BEGIN
        SELECT user_id INTO admin_user_id FROM public.users WHERE email = 'admin@smartparking.com';

        IF admin_user_id IS NULL THEN
            RAISE EXCEPTION 'Không tìm thấy user admin@smartparking.com trong bảng users';
        END IF;

        IF EXISTS (SELECT 1 FROM public.employees WHERE user_id = admin_user_id) THEN
            UPDATE public.employees
            SET role       = 'ADMIN',
                status     = 'ACTIVE',
                updated_at = NOW()
            WHERE user_id = admin_user_id;
        ELSE
            INSERT INTO public.employees (user_id, role, status, created_at, updated_at)
            VALUES (admin_user_id, 'ADMIN', 'ACTIVE', NOW(), NOW());
        END IF;
    END
$$;

-- -----------------------------------------------------------------------------
-- Bước 3: Kiểm tra kết quả
-- -----------------------------------------------------------------------------
SELECT u.user_id, u.email, u.full_name, e.role, e.status
FROM public.users u
         LEFT JOIN public.employees e ON e.user_id = u.user_id
WHERE u.email = 'admin@smartparking.com';
