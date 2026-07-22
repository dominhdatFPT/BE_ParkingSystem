-- =============================================================================
-- Migration: Thêm bảng VNPay và cập nhật fee_subscription_invoice
-- Chạy một lần trên PostgreSQL (Supabase)
-- =============================================================================

-- 1. Tạo bảng vnpay_order
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vnpay_order (
    txn_ref             VARCHAR(50)  PRIMARY KEY,
    user_id             BIGINT,
    subscription_id     BIGINT,
    invoice_id          BIGINT,
    amount              BIGINT       NOT NULL,          -- Số tiền VNĐ (chưa × 100)
    order_info          VARCHAR(500),
    payment_url         TEXT,                           -- Link redirect sang VNPay
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING/PAID/FAILED/CANCELLED
    vnp_transaction_no  VARCHAR(100),                   -- Mã giao dịch VNPay trả về
    vnp_response_code   VARCHAR(10),                    -- "00" = thành công
    vnp_bank_code       VARCHAR(20),                    -- Ngân hàng thực hiện
    created_at          TIMESTAMP    DEFAULT NOW(),
    expired_at          TIMESTAMP,
    paid_at             TIMESTAMP
);

COMMENT ON TABLE  vnpay_order IS 'Lưu trạng thái đơn hàng thanh toán qua VNPay';
COMMENT ON COLUMN vnpay_order.amount IS 'Số tiền VNĐ thực tế (chưa nhân 100 như VNPay yêu cầu)';
COMMENT ON COLUMN vnpay_order.status IS 'PENDING | PAID | FAILED | CANCELLED';

-- 2. Them cot VNPay vao fee_subscription_invoice
-- -----------------------------------------------------------------------------
ALTER TABLE fee_subscription_invoice
    ADD COLUMN IF NOT EXISTS vnp_txn_ref        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vnp_transaction_no VARCHAR(100);

COMMENT ON COLUMN fee_subscription_invoice.vnp_txn_ref        IS 'vnp_TxnRef gửi lên VNPay, ref đến bảng vnpay_order';
COMMENT ON COLUMN fee_subscription_invoice.vnp_transaction_no IS 'vnp_TransactionNo VNPay trả về (dùng để tra soát)';

-- 3. Index hỗ trợ lookup theo subscription_id và trạng thái
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_vnpay_order_subscription ON vnpay_order(subscription_id);
CREATE INDEX IF NOT EXISTS idx_vnpay_order_status       ON vnpay_order(status);
CREATE INDEX IF NOT EXISTS idx_vnpay_order_user         ON vnpay_order(user_id);
