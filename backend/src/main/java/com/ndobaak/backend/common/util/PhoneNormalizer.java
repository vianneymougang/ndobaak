package com.ndobaak.backend.common.util;

public final class PhoneNormalizer {

    private static final String DEFAULT_COUNTRY_CODE = "237";

    private PhoneNormalizer() {}

    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) return phone;

        String cleaned = phone.replaceAll("[\\s\\-().]+", "");

        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2);
        }

        if (cleaned.startsWith(DEFAULT_COUNTRY_CODE) && cleaned.length() >= 11) {
            return "+" + cleaned;
        }

        return "+" + DEFAULT_COUNTRY_CODE + cleaned;
    }
}
