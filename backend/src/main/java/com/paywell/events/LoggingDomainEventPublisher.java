package com.paywell.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default publisher: Kafka is optional, so events are only counted/logged at debug level. */
public class LoggingDomainEventPublisher implements DomainEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        log.debug("domain event type={} aggregate={}", event.type(), event.aggregateId());
    }
}
