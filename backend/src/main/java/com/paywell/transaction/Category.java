package com.paywell.transaction;

import java.util.Locale;

public enum Category {
    FOOD, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, HEALTH, TRANSFERS, TOPUP, REWARDS, OTHER;

    public static Category parseOrDefault(String s, Category fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
