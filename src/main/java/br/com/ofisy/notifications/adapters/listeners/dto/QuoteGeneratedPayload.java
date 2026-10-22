package br.com.ofisy.notifications.adapters.listeners.dto;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record QuoteGeneratedPayload(UUID quoteId, UUID serviceOrderId, BigDecimal totalPrice) {
    public QuoteGeneratedPayload {
        Objects.requireNonNull(quoteId, "quoteId is required");
        Objects.requireNonNull(serviceOrderId, "serviceOrderId is required");
        Objects.requireNonNull(totalPrice, "totalPrice is required");
    }
}