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
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parking_order_payments (
                    payment_id bigserial PRIMARY KEY,
                    order_id bigint NOT NULL REFERENCES parking_orders(order_id),
                    amount numeric(19, 2) NOT NULL,
                    payment_method varchar(30) NOT NULL,
                    payment_status varchar(30) NOT NULL,
                    received_by bigint REFERENCES users(user_id),
                    paid_at timestamp,
                    transaction_reference varchar(120),
                    notes text,
                    created_at timestamp NOT NULL DEFAULT now(),
                    updated_at timestamp NOT NULL DEFAULT now()
                )
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uq_parking_order_payment
                ON parking_order_payments(order_id)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE parking_order_payments
                DROP CONSTRAINT IF EXISTS chk_parking_payment_method
                """);
        jdbcTemplate.execute("""
                ALTER TABLE parking_order_payments
                ADD CONSTRAINT chk_parking_payment_method
                CHECK (payment_method IN ('CASH', 'MOMO', 'BANK_TRANSFER', 'STRIPE')) NOT VALID
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS stripe_order (
                    payment_intent_id varchar(100) PRIMARY KEY,
                    user_id bigint,
                    subscription_id bigint,
                    invoice_id bigint,
                    parking_order_id bigint,
                    order_type varchar(30) DEFAULT 'SUBSCRIPTION',
                    amount bigint NOT NULL,
                    currency varchar(10),
                    description varchar(500),
                    client_secret text,
                    status varchar(20) NOT NULL,
                    stripe_charge_id varchar(100),
                    failure_message varchar(500),
                    created_at timestamp DEFAULT now(),
                    expired_at timestamp,
                    paid_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                ALTER TABLE stripe_order
                ADD COLUMN IF NOT EXISTS parking_order_id BIGINT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE stripe_order
                ADD COLUMN IF NOT EXISTS order_type VARCHAR(30) DEFAULT 'SUBSCRIPTION'
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_stripe_order_parking_order_id
                ON stripe_order(parking_order_id)
                """);
        log.info("Support incident schema is ready");
    }
}
