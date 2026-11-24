package com.paywell.rewards;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RewardServiceTest {
    @Test
    void noCashbackBelowMinimumPayment() {
        assertThat(RewardService.cashbackFor(new BigDecimal("99.99"))).isEqualByComparingTo("0.00");
    }

    @Test
    void halfPercentRoundedDown() {
        assertThat(RewardService.cashbackFor(new BigDecimal("100.00"))).isEqualByComparingTo("0.50");
        assertThat(RewardService.cashbackFor(new BigDecimal("333.33"))).isEqualByComparingTo("1.66");   // 1.66665 -> 1.66
    }

    @Test
    void cappedAtTwenty() {
        assertThat(RewardService.cashbackFor(new BigDecimal("50000.00"))).isEqualByComparingTo("20.00");
    }
}
