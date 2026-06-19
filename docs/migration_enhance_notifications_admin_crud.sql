-- Migration: Enhance notifications table for admin CRUD (Plan A - broadcast)
-- Adds priority, recipient target, status and scheduled time columns.
-- Safe to re-run: each statement is guarded with IF NOT EXISTS.

-- =========================================================================
-- 1) Add missing columns required by the admin UI
-- =========================================================================
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS priority          VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS recipient_target  VARCHAR(50)  NOT NULL DEFAULT 'ALL_USERS',
    ADD COLUMN IF NOT EXISTS status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS scheduled_at      TIMESTAMP    NULL;

-- =========================================================================
-- 2) Update enum/category values to match the frontend labels
--    Existing data is migrated from old enum values to the new ones.
-- =========================================================================
UPDATE notifications
    SET category = 'HE_THONG'
    WHERE category IN ('THONG_TIN', 'CHINH_SACH', 'CANH_BAO')
      AND category <> 'BAO_TRI';

UPDATE notifications
    SET category = 'SU_CO'
    WHERE category = 'CANH_BAO';

-- =========================================================================
-- 3) Indexes for admin listing / filtering
-- =========================================================================
CREATE INDEX IF NOT EXISTS idx_notifications_status
    ON notifications (status);

CREATE INDEX IF NOT EXISTS idx_notifications_category
    ON notifications (category);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_target
    ON notifications (recipient_target);

COMMENT ON COLUMN notifications.priority         IS 'Enum: NORMAL | IMPORTANT';
COMMENT ON COLUMN notifications.recipient_target IS 'Enum: ALL_USERS | ACTIVE_PARKING | MONTHLY_SUBSCRIBERS | EXPIRING_SOON | SPECIFIC_USER';
COMMENT ON COLUMN notifications.status           IS 'Enum: DRAFT | SCHEDULED | SENT';
COMMENT ON COLUMN notifications.scheduled_at     IS 'When the notification should be published (only meaningful when status = SCHEDULED).';
