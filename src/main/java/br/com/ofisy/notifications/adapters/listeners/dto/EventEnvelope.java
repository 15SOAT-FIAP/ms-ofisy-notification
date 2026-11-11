package br.com.ofisy.notifications.adapters.listeners.dto;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Shared wire contract. Transport types remain outside application and domain. */
public record EventEnvelope(
        UUID eventId,
        String type,
        UUID serviceOrderId,
        Instant occurredAt,
        JsonNode payload
) {
    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(payload, "payload is required");
    }
}