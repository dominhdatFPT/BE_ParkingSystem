package com.swp.parking.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum NotificationCategory {
    HE_THONG("Hệ thống"),
    BAO_TRI("Bảo trì"),
    GOI_GUI_XE("Gói gửi xe"),
    THANH_TOAN("Thanh toán"),
    SU_CO("Sự cố");

    private final String label;

    NotificationCategory(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static NotificationCategory fromLabel(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(c -> c.label.equalsIgnoreCase(value) || c.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown notification category: " + value));
    }
}
