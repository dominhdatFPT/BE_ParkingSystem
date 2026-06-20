package com.swp.parking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                ALTER TABLE parking_incidents
                ADD COLUMN IF NOT EXISTS reply_title VARCHAR(200)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE notifications
                ADD COLUMN IF NOT EXISTS recipient_user_id BIGINT
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id
                ON notifications(recipient_user_id)
                """);
        log.info("Support incident schema is ready");
    }
}
