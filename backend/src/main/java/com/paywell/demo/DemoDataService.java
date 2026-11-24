package com.paywell.demo;

import com.paywell.common.Money;
import com.paywell.config.PaywellProperties;
import com.paywell.transaction.Category;
import com.paywell.transaction.Direction;
import com.paywell.transaction.TxType;
import com.paywell.wallet.Account;
import com.paywell.wallet.WalletService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a NEW account with clearly synthetic history (fictional merchants) so Money Radar and Insights have something to show.
 * Runs only when paywell.demo.enabled=true. Every row is flagged simulated and no real data is involved.
 */
@Service
public class DemoDataService {
    private record Seed(int daysAgo, Direction dir, TxType type, Category cat, String amount, String who, String note) {}

    private static final List<Seed> SEEDS = List.of(
            new Seed(70, Direction.DEBIT, TxType.BILL, Category.BILLS, "129.00", "Cloud Drive Plus", "Monthly plan"),
            new Seed(63, Direction.DEBIT, TxType.BILL, Category.ENTERTAINMENT, "499.00", "StreamBox", "Monthly plan"),
            new Seed(40, Direction.DEBIT, TxType.BILL, Category.BILLS, "129.00", "Cloud Drive Plus", "Monthly plan"),
            new Seed(33, Direction.DEBIT, TxType.BILL, Category.ENTERTAINMENT, "499.00", "StreamBox", "Monthly plan"),
            new Seed(20, Direction.DEBIT, TxType.P2P_SEND, Category.FOOD, "720.00", "Spice Route Cafe", null),
            new Seed(18, Direction.DEBIT, TxType.P2P_SEND, Category.SHOPPING, "1299.00", "Urban Threads", null),
            new Seed(15, Direction.CREDIT, TxType.P2P_RECEIVE, Category.TRANSFERS, "1500.00", "Demo Friend", "Dinner split"),
            new Seed(14, Direction.DEBIT, TxType.P2P_SEND, Category.HEALTH, "850.00", "CareFirst Pharmacy", null),
            new Seed(12, Direction.DEBIT, TxType.P2P_SEND, Category.FOOD, "275.00", "Spice Route Cafe", null),
            new Seed(11, Direction.DEBIT, TxType.P2P_SEND, Category.TRANSPORT, "310.00", "City Cab", null),
            new Seed(10, Direction.DEBIT, TxType.BILL, Category.BILLS, "129.00", "Cloud Drive Plus", "Monthly plan"),
            new Seed(9, Direction.DEBIT, TxType.P2P_SEND, Category.SHOPPING, "2399.00", "Urban Threads", null),
            new Seed(8, Direction.DEBIT, TxType.P2P_SEND, Category.FOOD, "540.00", "Spice Route Cafe", null),
            new Seed(4, Direction.DEBIT, TxType.P2P_SEND, Category.TRANSPORT, "190.00", "City Cab", null),
            new Seed(3, Direction.DEBIT, TxType.BILL, Category.ENTERTAINMENT, "499.00", "StreamBox", "Monthly plan"),
            new Seed(2, Direction.DEBIT, TxType.P2P_SEND, Category.SHOPPING, "6800.00", "Gadget Hub", null),
            new Seed(2, Direction.DEBIT, TxType.P2P_SEND, Category.FOOD, "610.00", "Spice Route Cafe", null),
            new Seed(1, Direction.DEBIT, TxType.P2P_SEND, Category.FOOD, "350.00", "Spice Route Cafe", null));

    private final WalletService wallet;
    private final PaywellProperties props;
    private final Clock clock;

    public DemoDataService(WalletService wallet, PaywellProperties props, Clock clock) {
        this.wallet = wallet;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seed(Account account) {
        if (!props.demo().enabled()) return;
        Instant now = Instant.now(clock);
        wallet.postSimple(account, Direction.CREDIT, TxType.TOPUP, Category.TOPUP, Money.requirePositive(props.demo().openingBalance()),
                "PAYWELL demo funds", null, "SIMULATED opening balance", now.minus(Duration.ofDays(75)));
        for (Seed s : SEEDS) {
            BigDecimal amount = new BigDecimal(s.amount());
            if (s.dir() == Direction.DEBIT && account.getBalance().compareTo(amount) < 0) continue;
            Instant at = now.minus(Duration.ofDays(s.daysAgo())).minus(Duration.ofMinutes(37L * s.amount().length()));
            wallet.postSimple(account, s.dir(), s.type(), s.cat(), amount, s.who(), null, s.note(), at);
        }
    }
}
