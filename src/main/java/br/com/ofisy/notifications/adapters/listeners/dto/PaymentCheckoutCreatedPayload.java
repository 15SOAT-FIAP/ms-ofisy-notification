package br.com.ofisy.notifications.adapters.listeners.dto;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentCheckoutCreatedPayload(UUID paymentId, String checkoutUrl, BigDecimal amount) {
    public PaymentCheckoutCreatedPayload {
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(checkoutUrl, "checkoutUrl is required");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
