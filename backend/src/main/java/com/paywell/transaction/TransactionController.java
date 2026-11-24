package com.paywell.transaction;

import com.paywell.common.PageResponse;
import com.paywell.security.CurrentUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<TransactionView> list(
            @RequestParam(required = false) TxType type,
            @RequestParam(required = false) Direction direction,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) TxStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(CurrentUser.id(), new TransactionService.Filter(type, direction, category, status, from, to, query), page, size);
    }

    @GetMapping("/{id}")
    public TransactionView get(@PathVariable UUID id) {
        return service.get(CurrentUser.id(), id);
    }
}
