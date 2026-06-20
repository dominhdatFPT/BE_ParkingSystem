CREATE TABLE IF NOT EXISTS system_configuration (
    config_id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (config_id = 1),
    config_data JSONB NOT NULL,
    updated_by BIGINT REFERENCES users(user_id),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_configuration (config_id, config_data)
VALUES (1, '{"systemName":"Smart Parking AI","region":"VN","timezone":"Asia/Ho_Chi_Minh","defaultLanguage":"Vietnamese","operationMode":"auto","enableCamera":true,"enableRFID":true,"enableQR":false,"enablePaymentGateway":false,"enableAuditLogs":true,"sessionTimeout":15,"maxConcurrentSessions":120,"maintenanceWindow":"02:00 - 04:00"}'::jsonb)
ON CONFLICT (config_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS parking_incidents (
    incident_id BIGSERIAL PRIMARY KEY,
    incident_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    license_plate VARCHAR(30),
    card_code VARCHAR(50),
    description TEXT NOT NULL,
    resolution TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT REFERENCES users(user_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_by BIGINT REFERENCES users(user_id),
    resolved_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    action VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_parking_incidents_created_at ON parking_incidents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
