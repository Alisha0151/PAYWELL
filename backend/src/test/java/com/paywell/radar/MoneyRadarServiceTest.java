package com.paywell.radar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.paywell.TestProps;
import com.paywell.insights.BudgetRepository;
import com.paywell.radar.RadarDtos.Anomaly;
import com.paywell.radar.RadarDtos.Subscription;
import com.paywell.transaction.Category;
import com.paywell.transaction.Direction;
import com.paywell.transaction.Transaction;
import com.paywell.transaction.TransactionRepository;
import com.paywell.transaction.TxStatus;
import com.paywell.transaction.TxType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MoneyRadarServiceTest {
    private final MoneyRadarService service = new MoneyRadarService(mock(TransactionRepository.class), mock(BudgetRepository.class),
            TestProps.CLOCK, ZoneId.of("Asia/Kolkata"));
    private final Instant now = TestProps.CLOCK.instant();
    private final UUID user = UUID.randomUUID();

    private Transaction debit(int daysAgo, String who, String amount, Category cat) {
        return new Transaction(UUID.randomUUID(), "PW26110000" + String.format("%04d", Math.abs((who + amount + daysAgo).hashCode()) % 10000),
                user, UUID.randomUUID(), Direction.DEBIT, TxType.P2P_SEND, cat, new BigDecimal(amount), "INR", TxStatus.SUCCESS,
                who, null, null, null, now.minus(Duration.ofDays(daysAgo)).minus(Duration.ofHours(1)));
    }

    @Test
    void detectsMonthlySubscriptionWithSteadyAmounts() {
        List<Transaction> list = List.of(debit(63, "StreamBox", "499.00", Category.ENTERTAINMENT), debit(33, "StreamBox", "499.00", Category.ENTERTAINMENT),
                debit(3, "StreamBox", "499.00", Category.ENTERTAINMENT), debit(5, "Cafe", "300.00", Category.FOOD));
        List<Subscription> subs = service.subscriptions(list);
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).frequency()).isEqualTo("MONTHLY");
        assertThat(subs.get(0).amount()).isEqualByComparingTo("499.00");
        assertThat(subs.get(0).occurrences()).isEqualTo(3);
    }

    @Test
    void irregularOrVaryingChargesAreNotSubscriptions() {
        List<Transaction> varying = List.of(debit(60, "Cafe", "100.00", Category.FOOD), debit(30, "Cafe", "400.00", Category.FOOD), debit(2, "Cafe", "900.00", Category.FOOD));
        assertThat(service.subscriptions(varying)).isEmpty();
        List<Transaction> irregular = List.of(debit(50, "Shop", "200.00", Category.SHOPPING), debit(47, "Shop", "200.00", Category.SHOPPING), debit(2, "Shop", "200.00", Category.SHOPPING));
        assertThat(service.subscriptions(irregular)).isEmpty();
    }

    @Test
    void flagsPaymentFarLargerThanTypical() {
        List<Transaction> list = new ArrayList<>();
        for (int i = 20; i >= 8; i -= 3) list.add(debit(i, "Cafe" + i, "300.00", Category.FOOD));
        list.add(debit(2, "Gadget Hub", "6800.00", Category.SHOPPING));
        List<Anomaly> a = service.anomalies(list, now.minus(Duration.ofDays(30)));
        assertThat(a).anySatisfy(x -> {
            assertThat(x.kind()).isIn("LARGE_VS_TYPICAL", "NEW_COUNTERPARTY_LARGE");
            assertThat(x.amount()).isEqualByComparingTo("6800.00");
        });
        assertThat(a.get(0).severity()).isEqualTo("HIGH");
    }

    @Test
    void ordinaryActivityRaisesNoAnomalies() {
        List<Transaction> list = new ArrayList<>();
        for (int i = 25; i >= 5; i -= 4) list.add(debit(i, "Cafe", "250.00", Category.FOOD));
        assertThat(service.anomalies(list, now.minus(Duration.ofDays(30)))).isEmpty();
    }

    @Test
    void categoriesAreSortedAndSharesAddUp() {
        List<Transaction> period = List.of(debit(3, "A", "300.00", Category.FOOD), debit(4, "B", "100.00", Category.TRANSPORT), debit(5, "C", "600.00", Category.SHOPPING));
        var cats = service.categories(period, List.of(debit(40, "A", "100.00", Category.FOOD)), new BigDecimal("1000.00"));
        assertThat(cats.get(0).category()).isEqualTo("SHOPPING");
        assertThat(cats.get(0).sharePercent()).isEqualByComparingTo("60.0");
        assertThat(cats.stream().filter(c -> c.category().equals("FOOD")).findFirst().get().changePercent()).isEqualByComparingTo("200.0");
    }
}
