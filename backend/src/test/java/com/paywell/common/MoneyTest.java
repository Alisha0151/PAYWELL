package com.paywell.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void acceptsTwoDecimalPlacesAndNormalisesScale() {
        assertThat(Money.requirePositive(new BigDecimal("10"))).isEqualByComparingTo("10.00").extracting(BigDecimal::scale).isEqualTo(2);
        assertThat(Money.requirePositive(new BigDecimal("0.01"))).isEqualByComparingTo("0.01");
        assertThat(Money.requirePositive(new BigDecimal("12.50"))).isEqualByComparingTo("12.50");
    }

    @Test
    void trailingZerosBeyondTwoDecimalsAreHarmless() {
        assertThat(Money.requirePositive(new BigDecimal("5.500000"))).isEqualByComparingTo("5.50");
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal("1.001")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_AMOUNT));
    }

    @Test
    void rejectsZeroNegativeAndNull() {
        for (BigDecimal bad : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-1.00"), null}) {
            assertThatThrownBy(() -> Money.requirePositive(bad))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_AMOUNT));
        }
    }

    @Test
    void enforcesPerPaymentLimit() {
        BigDecimal max = new BigDecimal("50000.00");
        assertThat(Money.requireWithinLimit(new BigDecimal("50000.00"), max)).isEqualByComparingTo("50000.00");
        assertThatThrownBy(() -> Money.requireWithinLimit(new BigDecimal("50000.01"), max))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.AMOUNT_LIMIT_EXCEEDED));
    }

    @Test
    void sumIsExactWhereDoublesAreNot() {
        // 0.1 + 0.2 != 0.3 in binary floating point; BigDecimal must be exact.
        assertThat(Money.sum(List.of(new BigDecimal("0.10"), new BigDecimal("0.20")))).isEqualByComparingTo("0.30");
    }

    @Test
    void percentHandlesZeroWhole() {
        assertThat(Money.percent(new BigDecimal("5.00"), BigDecimal.ZERO)).isEqualByComparingTo("0.0");
        assertThat(Money.percent(new BigDecimal("25.00"), new BigDecimal("200.00"))).isEqualByComparingTo("12.5");
    }
}
