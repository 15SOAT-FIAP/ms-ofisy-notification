package br.com.ofisy.notifications.adapters.listeners.dto;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record QuoteSentForApprovalPayload(UUID quoteId, BigDecimal totalPrice) {
    public QuoteSentForApprovalPayload {
        Objects.requireNonNull(quoteId, "quoteId is required");
        Objects.requireNonNull(totalPrice, "totalPrice is required");
    }
}
