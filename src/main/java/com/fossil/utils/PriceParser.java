package com.fossil.utils;

import java.math.BigDecimal;

/**
 * Converts displayed prices such as "₹ 11,995.00" into BigDecimal so money
 * comparisons are exact (no floating point drift).
 */
public final class PriceParser {

    private PriceParser() {
    }

    public static BigDecimal parse(String displayed) {
        if (displayed == null || displayed.isBlank()) {
            throw new IllegalArgumentException("Price text is empty");
        }
        String text = displayed.trim();
        if (text.equalsIgnoreCase("FREE")) {
            return BigDecimal.ZERO.setScale(2);
        }
        String numeric = text.replaceAll("[^0-9.]", "");
        if (numeric.isEmpty()) {
            throw new IllegalArgumentException("No numeric value in price text: '" + displayed + "'");
        }
        return new BigDecimal(numeric).setScale(2);
    }
}
