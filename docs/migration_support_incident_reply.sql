ALTER TABLE parking_incidents
    ADD COLUMN IF NOT EXISTS reply_title VARCHAR(200);

COMMENT ON COLUMN parking_incidents.reply_title
    IS 'Tiêu đề phản hồi của staff/admin cho yêu cầu hỗ trợ';

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS recipient_user_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id
    ON notifications(recipient_user_id);

COMMENT ON COLUMN notifications.recipient_user_id
    IS 'User cụ thể nhận thông báo; NULL đối với thông báo broadcast';
