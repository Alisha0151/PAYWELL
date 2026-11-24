package com.paywell.insights;

import com.paywell.insights.InsightsDtos.BillView;
import com.paywell.insights.InsightsDtos.BudgetView;
import com.paywell.insights.InsightsDtos.CreateBillRequest;
import com.paywell.insights.InsightsDtos.InsightsSummary;
import com.paywell.insights.InsightsDtos.SetBudgetRequest;
import com.paywell.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightsController {
    private final InsightsService service;

    public InsightsController(InsightsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public InsightsSummary summary(@RequestParam(defaultValue = "6") int months) {
        return service.summary(CurrentUser.id(), months);
    }

    @GetMapping("/budgets")
    public List<BudgetView> budgets() { return service.budgets(CurrentUser.id()); }

    @PutMapping("/budgets/{category}")
    public BudgetView setBudget(@PathVariable String category, @Valid @RequestBody SetBudgetRequest r) {
        return service.setBudget(CurrentUser.id(), category, r.monthlyLimit());
    }

    @GetMapping("/bills")
    public List<BillView> bills() { return service.bills(CurrentUser.id()); }

    @PostMapping("/bills")
    @ResponseStatus(HttpStatus.CREATED)
    public BillView addBill(@Valid @RequestBody CreateBillRequest r) { return service.addBill(CurrentUser.id(), r); }

    @DeleteMapping("/bills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBill(@PathVariable UUID id) { service.deleteBill(CurrentUser.id(), id); }
}
