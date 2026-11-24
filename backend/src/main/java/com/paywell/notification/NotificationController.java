package com.paywell.notification;

import com.paywell.security.CurrentUser;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationService.Inbox inbox(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.inbox(CurrentUser.id(), page, size);
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> read(@PathVariable UUID id) {
        service.markRead(CurrentUser.id(), id);
        return Map.of("read", true);
    }

    @PostMapping("/read-all")
    public Map<String, Object> readAll() {
        return Map.of("updated", service.markAllRead(CurrentUser.id()));
    }
}
