package com.paywell.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money rules in one place. All amounts are BigDecimal with scale 2; floating point is never used. */
public final class Money {
    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);
    public static final BigDecimal ONE_PAISA = new BigDecimal("0.01");

    private Money() {}

    public static BigDecimal scale(BigDecimal v) {
        return v.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    /** Accepts only positive amounts with at most two decimal places; returns the value at scale 2. */
    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null) {
            throw new ApiException(ErrorCode.INVALID_AMOUNT, "Amount is required.");
        }
        if (amount.signum() <= 0) {
            throw new ApiException(ErrorCode.INVALID_AMOUNT, "Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new ApiException(ErrorCode.INVALID_AMOUNT, "Amount can have at most two decimal places.");
        }
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal requireWithinLimit(BigDecimal amount, BigDecimal max) {
        BigDecimal a = requirePositive(amount);
        if (a.compareTo(max) > 0) {
            throw new ApiException(ErrorCode.AMOUNT_LIMIT_EXCEEDED,
                    "Amount exceeds the demo per-payment limit of " + max.setScale(SCALE, RoundingMode.UNNECESSARY).toPlainString() + ".");
        }
        return a;
    }

    public static BigDecimal sum(Iterable<BigDecimal> values) {
        BigDecimal total = ZERO;
        for (BigDecimal v : values) {
            total = total.add(v);
        }
        return total;
    }

    /** Percentage share rounded to one decimal place; zero when the whole is zero. */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }
}
