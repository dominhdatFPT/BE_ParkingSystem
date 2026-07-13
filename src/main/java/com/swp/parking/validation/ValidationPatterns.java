package com.swp.parking.validation;

public final class ValidationPatterns {

    public static final String GMAIL_EMAIL = "^[A-Z0-9._%+-]+@gmail\\.com$";
    public static final String STRONG_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    private ValidationPatterns() {
    }
}
