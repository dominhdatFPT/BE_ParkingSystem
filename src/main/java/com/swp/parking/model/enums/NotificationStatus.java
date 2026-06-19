package com.swp.parking.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum NotificationStatus {
    DRAFT("Nháp"),
    SCHEDULED("Hẹn lịch"),
    SENT("Đã gửi");

    private final String label;

    NotificationStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static NotificationStatus fromLabel(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.label.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown notification status: " + value));
    }
}
