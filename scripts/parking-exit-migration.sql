BEGIN;

CREATE TABLE IF NOT EXISTS visitor_fee_rates (
    fee_rate_id bigserial PRIMARY KEY,
    parking_id bigint REFERENCES parking_facilities(parking_id),
    vehicle_type_id bigint NOT NULL REFERENCES vehicle_types(vehicle_type_id),
    first_block_minutes integer NOT NULL,
    first_block_fee numeric(19, 2) NOT NULL,
    next_block_minutes integer NOT NULL,
    next_block_fee numeric(19, 2) NOT NULL,
    daily_cap numeric(19, 2),
    overnight_fee numeric(19, 2) NOT NULL DEFAULT 0,
    effective_from timestamp NOT NULL DEFAULT now(),
    effective_to timestamp,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_visitor_fee_rate_blocks CHECK (first_block_minutes > 0 AND next_block_minutes > 0),
    CONSTRAINT chk_visitor_fee_rate_amounts CHECK (
        first_block_fee >= 0 AND next_block_fee >= 0
        AND (daily_cap IS NULL OR daily_cap >= 0) AND overnight_fee >= 0
    ),
    CONSTRAINT chk_visitor_fee_rate_dates CHECK (effective_to IS NULL OR effective_to > effective_from)
);

ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS entry_type varchar(20);
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS vehicle_type_id bigint;
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS subscription_id bigint;
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS visitor_card_id bigint;
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS checked_out_by bigint;
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS checkout_confirmed_at timestamp;
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS payment_status varchar(30);
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS payment_method varchar(30);
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS fee_rate_id bigint;
ALTER TABLE parking_orders ADD COLUMN IF NOT EXISTS fee_breakdown jsonb;

UPDATE parking_orders
SET entry_type = CASE
    WHEN notes LIKE 'ENTRY_TYPE=MONTHLY%' THEN 'SUBSCRIPTION'
    WHEN notes LIKE 'ENTRY_TYPE=VISITOR%' THEN 'VISITOR'
    WHEN vehicle_id IS NOT NULL THEN 'SUBSCRIPTION'
    ELSE 'VISITOR'
END
WHERE entry_type IS NULL;

UPDATE parking_orders po
SET vehicle_type_id = COALESCE(
    (SELECT v.vehicle_type_id FROM vehicles v WHERE v.vehicle_id = po.vehicle_id),
    (SELECT vt.vehicle_type_id
       FROM vehicle_types vt
      WHERE po.notes LIKE '%VEHICLE_TYPE=' || vt.type_code || '%'
         OR (po.notes LIKE '%VEHICLE_TYPE=MOTORBIKE%' AND upper(vt.type_name) LIKE '%MOTOR%')
         OR (po.notes LIKE '%VEHICLE_TYPE=CAR%' AND upper(vt.type_name) NOT LIKE '%MOTOR%')
      ORDER BY vt.vehicle_type_id
      LIMIT 1)
)
WHERE po.vehicle_type_id IS NULL;

UPDATE parking_orders po
SET visitor_card_id = vc.visitor_card_id
FROM visitor_cards vc
WHERE vc.current_order_id = po.order_id
  AND po.visitor_card_id IS NULL;

UPDATE parking_orders po
SET subscription_id = (
    SELECT candidate.fee_subscription_id
      FROM fee_subscription candidate
     WHERE candidate.vehicle_id = po.vehicle_id
       AND candidate.status = 'ACTIVE'
       AND (candidate.start_date IS NULL OR candidate.start_date <= COALESCE(po.entry_time, now()))
       AND (candidate.end_date IS NULL OR candidate.end_date >= COALESCE(po.entry_time, now()))
     ORDER BY candidate.end_date DESC NULLS FIRST
     LIMIT 1
)
WHERE po.entry_type = 'SUBSCRIPTION'
  AND po.subscription_id IS NULL;

UPDATE parking_orders
SET payment_status = CASE
    WHEN entry_type = 'SUBSCRIPTION' THEN 'NOT_REQUIRED'
    WHEN parking_status = 'COMPLETED' AND calculated_fee IS NOT NULL THEN 'PAID'
    ELSE 'UNPAID'
END
WHERE payment_status IS NULL;

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
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_parking_order_payment UNIQUE (order_id),
    CONSTRAINT chk_parking_payment_amount CHECK (amount >= 0),
    CONSTRAINT chk_parking_payment_method CHECK (payment_method IN ('CASH', 'MOMO', 'BANK_TRANSFER')),
    CONSTRAINT chk_parking_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'FAILED', 'REFUNDED'))
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_orders_vehicle_type') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT fk_parking_orders_vehicle_type
            FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(vehicle_type_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_orders_subscription') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT fk_parking_orders_subscription
            FOREIGN KEY (subscription_id) REFERENCES fee_subscription(fee_subscription_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_orders_visitor_card') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT fk_parking_orders_visitor_card
            FOREIGN KEY (visitor_card_id) REFERENCES visitor_cards(visitor_card_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_orders_checked_in_by') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT fk_parking_orders_checked_in_by
            FOREIGN KEY (checked_in_by) REFERENCES users(user_id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_orders_checked_out_by') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT fk_parking_orders_checked_out_by
            FOREIGN KEY (checked_out_by) REFERENCES users(user_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_orders_fee_rate') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT fk_parking_orders_fee_rate
            FOREIGN KEY (fee_rate_id) REFERENCES visitor_fee_rates(fee_rate_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_visitor_cards_current_order') THEN
        ALTER TABLE visitor_cards ADD CONSTRAINT fk_visitor_cards_current_order
            FOREIGN KEY (current_order_id) REFERENCES parking_orders(order_id) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_parking_orders_entry_type') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT chk_parking_orders_entry_type
            CHECK (entry_type IN ('VISITOR', 'SUBSCRIPTION')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_parking_orders_payment_status') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT chk_parking_orders_payment_status
            CHECK (payment_status IN ('UNPAID', 'PAID', 'NOT_REQUIRED', 'FAILED', 'REFUNDED')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_parking_orders_status') THEN
        ALTER TABLE parking_orders ADD CONSTRAINT chk_parking_orders_status
            CHECK (parking_status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'ISSUE')) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_visitor_cards_status') THEN
        ALTER TABLE visitor_cards ADD CONSTRAINT chk_visitor_cards_status
            CHECK (status IN ('AVAILABLE', 'IN_USE', 'LOST', 'DISABLED')) NOT VALID;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_visitor_fee_rates_lookup
    ON visitor_fee_rates (vehicle_type_id, parking_id, is_active, effective_from, effective_to);

CREATE INDEX IF NOT EXISTS idx_parking_orders_normalized_plate_status
    ON parking_orders ((regexp_replace(upper(license_plate), '[^A-Z0-9]', '', 'g')), parking_status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_parking_orders_active_plate
    ON parking_orders ((regexp_replace(upper(license_plate), '[^A-Z0-9]', '', 'g')))
    WHERE parking_status = 'ACTIVE';

INSERT INTO visitor_fee_rates (
    parking_id, vehicle_type_id, first_block_minutes, first_block_fee,
    next_block_minutes, next_block_fee, daily_cap, overnight_fee,
    effective_from, is_active
)
SELECT NULL,
       vt.vehicle_type_id,
       60,
       CASE WHEN upper(COALESCE(vt.type_code, vt.type_name)) LIKE '%MOTOR%' THEN 5000 ELSE 20000 END,
       60,
       CASE WHEN upper(COALESCE(vt.type_code, vt.type_name)) LIKE '%MOTOR%' THEN 5000 ELSE 20000 END,
       NULL,
       0,
       timestamp '2026-01-01 00:00:00',
       true
FROM vehicle_types vt
WHERE NOT EXISTS (
    SELECT 1
    FROM visitor_fee_rates rate
    WHERE rate.vehicle_type_id = vt.vehicle_type_id
      AND rate.parking_id IS NULL
      AND rate.is_active = true
);

COMMIT;
