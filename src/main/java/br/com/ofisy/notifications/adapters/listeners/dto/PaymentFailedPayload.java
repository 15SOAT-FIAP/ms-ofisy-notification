package br.com.ofisy.notifications.adapters.listeners.dto;

import java.util.Objects;
import java.util.UUID;

public record PaymentFailedPayload(UUID paymentId, String reason, String statusDetail) {
    public PaymentFailedPayload {
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(reason, "reason is required");
    }
}
