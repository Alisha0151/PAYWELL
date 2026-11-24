package com.paywell.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Integration event. Payload carries identifiers and amounts only, never credentials or contact details. */
public record DomainEvent(String type, UUID userId, String aggregateId, Map<String, Object> payload, Instant occurredAt) {}
