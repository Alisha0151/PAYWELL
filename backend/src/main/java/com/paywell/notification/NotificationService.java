package com.paywell.notification;

import com.paywell.common.ApiException;
import com.paywell.common.ErrorCode;
import com.paywell.common.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** In-app notification inbox. (Push delivery is not implemented; nothing here is sent outside the app.) */
@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record NotificationView(UUID id, String kind, String title, String body, boolean read, Instant createdAt) {
        static NotificationView of(Notification n) {
            return new NotificationView(n.getId(), n.getKind(), n.getTitle(), n.getBody(), n.isRead(), n.getCreatedAt());
        }
    }

    public record Inbox(long unread, PageResponse<NotificationView> page) {}

    @Transactional
    public void notify(UUID userId, String kind, String title, String body) {
        repository.save(new Notification(userId, kind, title, body, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public Inbox inbox(UUID userId, int page, int size) {
        var p = repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new Inbox(repository.countByUserIdAndReadFalse(userId), PageResponse.of(p, NotificationView::of));
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification n = repository.findById(id).filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        n.markRead();
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId);
    }
}
