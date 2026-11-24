package com.paywell.radar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class Stats {
    private Stats() {}

    /** Median of BigDecimals; zero for an empty list. */
    static BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(2);
        List<BigDecimal> s = new ArrayList<>(values);
        s.sort(BigDecimal::compareTo);
        int n = s.size();
        if (n % 2 == 1) return s.get(n / 2).setScale(2, RoundingMode.HALF_UP);
        return s.get(n / 2 - 1).add(s.get(n / 2)).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    static double medianLong(List<Long> values) {
        if (values.isEmpty()) return 0;
        List<Long> s = new ArrayList<>(values);
        s.sort(Long::compare);
        int n = s.size();
        return n % 2 == 1 ? s.get(n / 2) : (s.get(n / 2 - 1) + s.get(n / 2)) / 2.0;
    }
}
