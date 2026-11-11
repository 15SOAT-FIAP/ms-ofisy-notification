package br.com.ofisy.notifications.adapters.listeners.dto;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentConfirmedPayload(UUID paymentId, BigDecimal amount) {
    public PaymentConfirmedPayload {
        Objects.requireNonNull(paymentId, "paymentId is required");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
