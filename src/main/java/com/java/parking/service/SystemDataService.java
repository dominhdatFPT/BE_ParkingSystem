package com.swp.parking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemDataService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getConfiguration() {
        String json = jdbcTemplate.queryForObject(
                "SELECT config_data::text FROM system_configuration WHERE config_id = 1", String.class);
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể đọc cấu hình hệ thống", exception);
        }
    }

    @Transactional
    public Map<String, Object> saveConfiguration(Long userId, Map<String, Object> config) {
        try {
            String json = objectMapper.writeValueAsString(config);
            jdbcTemplate.update("""
                    INSERT INTO system_configuration(config_id, config_data, updated_by, updated_at)
                    VALUES (1, ?::jsonb, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (config_id) DO UPDATE SET config_data = EXCLUDED.config_data,
                        updated_by = EXCLUDED.updated_by, updated_at = CURRENT_TIMESTAMP
                    """, json, userId);
            writeAudit(userId, "UPDATE_SYSTEM_CONFIGURATION", "SUCCESS", "MEDIUM", "Cập nhật cấu hình hệ thống");
            return getConfiguration();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể lưu cấu hình hệ thống", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getIncidents() {
        return jdbcTemplate.queryForList("""
                SELECT i.incident_id AS id, i.incident_type AS type, i.severity,
                       i.license_plate AS plate, i.card_code, i.description, i.resolution,
                       i.status, i.created_at, u.full_name AS created_by_name
                FROM parking_incidents i LEFT JOIN users u ON u.user_id = i.created_by
                ORDER BY i.created_at DESC
                """);
    }

    @Transactional
    public Map<String, Object> createIncident(Long userId, Map<String, Object> request) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO parking_incidents(incident_type, severity, license_plate, card_code,
                    description, resolution, created_by)
                VALUES (?, ?, NULLIF(?, ''), NULLIF(?, ''), ?, NULLIF(?, ''), ?)
                RETURNING incident_id
                """, Long.class, request.get("type"), request.get("severity"), request.get("plate"),
                request.get("cardCode"), request.get("description"), request.get("resolution"), userId);
        writeAudit(userId, "CREATE_INCIDENT", "SUCCESS", String.valueOf(request.get("severity")),
                "Tạo sự cố #" + id);
        return findIncident(id);
    }

    @Transactional
    public Map<String, Object> closeIncident(Long userId, Long id, String resolution) {
        int changed = jdbcTemplate.update("""
                UPDATE parking_incidents SET status = 'CLOSED', resolution = ?, resolved_by = ?,
                    resolved_at = CURRENT_TIMESTAMP WHERE incident_id = ? AND status = 'OPEN'
                """, resolution, userId, id);
        if (changed == 0) throw new IllegalArgumentException("Sự cố không tồn tại hoặc đã đóng");
        writeAudit(userId, "CLOSE_INCIDENT", "SUCCESS", "INFO", "Đóng sự cố #" + id);
        return findIncident(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAuditLogs() {
        return jdbcTemplate.queryForList("""
                SELECT a.audit_id AS id, a.action, a.status, a.severity, a.message,
                       a.created_at, u.full_name AS user_name, u.email
                FROM audit_logs a LEFT JOIN users u ON u.user_id = a.user_id
                ORDER BY a.created_at DESC LIMIT 500
                """);
    }

    private Map<String, Object> findIncident(Long id) {
        return jdbcTemplate.queryForMap("""
                SELECT incident_id AS id, incident_type AS type, severity, license_plate AS plate,
                       card_code, description, resolution, status, created_at
                FROM parking_incidents WHERE incident_id = ?
                """, id);
    }

    public void writeAudit(Long userId, String action, String status, String severity, String message) {
        jdbcTemplate.update("INSERT INTO audit_logs(user_id, action, status, severity, message) VALUES (?, ?, ?, ?, ?)",
                userId, action, status, severity, message);
    }
}
