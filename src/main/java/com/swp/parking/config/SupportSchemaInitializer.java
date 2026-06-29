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
        jdbcTemplate.execute("""
                ALTER TABLE parking_orders
                ADD COLUMN IF NOT EXISTS vehicle_type_id BIGINT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE parking_orders
                ADD COLUMN IF NOT EXISTS subscription_id BIGINT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE parking_orders
                ADD COLUMN IF NOT EXISTS visitor_card_id BIGINT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE parking_orders
                ADD COLUMN IF NOT EXISTS entry_type VARCHAR(30)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_parking_orders_updated_at_desc
                ON parking_orders(updated_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_parking_orders_status_updated
                ON parking_orders(parking_status, updated_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_parking_orders_entry_time
                ON parking_orders(entry_time)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_parking_orders_visitor_card_id
                ON parking_orders(visitor_card_id)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_visitor_cards_current_order
                ON visitor_cards(current_order_id)
                """);
        log.info("Support incident schema is ready");
    }
}
