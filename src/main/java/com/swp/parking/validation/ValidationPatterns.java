package com.swp.parking.validation;

public final class ValidationPatterns {

    public static final String GMAIL_EMAIL = "^[A-Z0-9._%+-]+@gmail\\.com$";
    public static final String STRONG_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    /** Applied to an already-normalized (uppercase, non-alphanumeric-dash stripped) plate. */
    public static final String LICENSE_PLATE = "^(?=.*[A-Z])(?=.*\\d)[A-Z0-9]{5,12}$";

    private ValidationPatterns() {
    }
}
