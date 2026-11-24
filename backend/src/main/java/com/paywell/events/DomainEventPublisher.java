package com.paywell.events;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
