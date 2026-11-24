package com.paywell.events;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Publishes events only after the surrounding transaction commits, and never lets a publisher failure escape. */
@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final DomainEventPublisher publisher;
    private final Clock clock;

    public EventService(DomainEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    public void publish(String type, UUID userId, String aggregateId, Map<String, Object> payload) {
        DomainEvent event = new DomainEvent(type, userId, aggregateId, payload, Instant.now(clock));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safe(event);
                }
            });
        } else {
            safe(event);
        }
    }

    private void safe(DomainEvent event) {
        try {
            publisher.publish(event);
        } catch (RuntimeException e) {
            log.warn("Event publisher failed type={} reason={}", event.type(), e.getClass().getSimpleName());
        }
    }
}
