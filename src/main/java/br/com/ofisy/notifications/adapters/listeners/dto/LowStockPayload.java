package br.com.ofisy.notifications.adapters.listeners.dto;

import java.util.Objects;
import java.util.UUID;

public record LowStockPayload(UUID stockId, String productName, Integer currentQuantity, Integer minThreshold) {
    public LowStockPayload {
        Objects.requireNonNull(stockId, "stockId is required");
        Objects.requireNonNull(productName, "productName is required");
        Objects.requireNonNull(currentQuantity, "currentQuantity is required");
        Objects.requireNonNull(minThreshold, "minThreshold is required");
    }
}