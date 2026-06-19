package com.swp.parking.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum NotificationRecipientTarget {
    ALL_USERS("Tất cả user"),
    ACTIVE_PARKING("User đang gửi xe"),
    MONTHLY_SUBSCRIBERS("User có gói tháng"),
    EXPIRING_SOON("User sắp hết hạn gói"),
    SPECIFIC_USER("User cụ thể");

    private final String label;

    NotificationRecipientTarget(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static NotificationRecipientTarget fromLabel(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(t -> t.label.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown recipient target: " + value));
    }
}
