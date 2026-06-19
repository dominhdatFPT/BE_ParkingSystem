package com.swp.parking.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum NotificationPriority {
    NORMAL("Thường"),
    IMPORTANT("Quan trọng");

    private final String label;

    NotificationPriority(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static NotificationPriority fromLabel(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(p -> p.label.equalsIgnoreCase(value) || p.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown notification priority: " + value));
    }
}
