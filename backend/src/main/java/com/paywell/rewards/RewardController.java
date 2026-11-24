package com.paywell.rewards;

import com.paywell.security.CurrentUser;
import com.paywell.transaction.TransactionView;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards")
public class RewardController {
    private final RewardService service;

    public RewardController(RewardService service) {
        this.service = service;
    }

    @GetMapping
    public RewardService.RewardsSummary list() {
        return service.summary(CurrentUser.id());
    }

    @PostMapping("/{id}/redeem")
    public TransactionView redeem(@PathVariable UUID id) {
        return service.redeem(CurrentUser.id(), id);
    }
}
