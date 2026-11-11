package br.com.ofisy.notifications.domain.notification;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

public class Notification {

    private final UUID id;
    private final NotificationType type;
    private final UUID stockId;
    private final UUID quoteId;
    private final UUID paymentId;
    private final String checkoutUrl;
    private final UUID customerId;
    private final UUID serviceOrderId;
    private final NotificationMessage message;
    private boolean read;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Notification(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.stockId = builder.stockId;
        this.quoteId = builder.quoteId;
        this.paymentId = builder.paymentId;
        this.checkoutUrl = builder.checkoutUrl;
        this.customerId = builder.customerId;
        this.serviceOrderId = builder.serviceOrderId;
        this.message = builder.message;
        this.read = builder.read;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }


    public static Notification createForStock(UUID stockId, String message) {
        return createForStock(stockId, NotificationMessage.fromString(message));
    }
    public static Notification createForStock(UUID stockId, NotificationMessage message) {
        return createForStock(UUID.randomUUID(), stockId, message);
    }

    public static Notification createForQuote(UUID quoteId, String message) {
        return createForQuote(quoteId, NotificationMessage.fromString(message));
    }
    public static Notification createForQuote(UUID quoteId, NotificationMessage message) {
        return createForQuote(UUID.randomUUID(), quoteId, message);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Notification createForStock(UUID id, UUID stockId, NotificationMessage message) {
        LocalDateTime now = LocalDateTime.now();
        return builder()
                .id(id)
                .type(NotificationType.LOW_STOCK)
                .stockId(stockId)
                .message(message)
                .read(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static Notification createForQuote(UUID id, UUID quoteId, NotificationMessage message) {
        return createForQuote(id, quoteId, message, null, null);
    }

    public static Notification createForQuote(UUID id, UUID quoteId, NotificationMessage message,
                                              UUID serviceOrderId, UUID customerId) {
        LocalDateTime now = LocalDateTime.now();
        return builder()
                .id(id)
                .type(NotificationType.QUOTE_GENERATED)
                .quoteId(quoteId)
                .serviceOrderId(serviceOrderId)
                .customerId(customerId)
                .message(message)
                .read(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void markAsRead() {
        this.read = true;
        this.updatedAt = LocalDateTime.now();
    }

    public static Notification createForPayment(UUID id, NotificationType type, UUID paymentId,
                                               UUID serviceOrderId, UUID customerId,
                                               NotificationMessage message, String checkoutUrl) {
        switch (type) {
            case PAYMENT_CHECKOUT_CREATED -> Objects.requireNonNull(checkoutUrl, "checkoutUrl is required");
            case PAYMENT_CONFIRMED, PAYMENT_FAILED -> {
                if (checkoutUrl != null) {
                    throw new IllegalArgumentException("Only checkout notifications carry a checkout URL");
                }
            }
            case LOW_STOCK, QUOTE_GENERATED -> throw new IllegalArgumentException("Payment notification type is required");
        }
        var now = LocalDateTime.now();
        return builder().id(id).type(type).paymentId(Objects.requireNonNull(paymentId))
                .serviceOrderId(Objects.requireNonNull(serviceOrderId))
                .customerId(Objects.requireNonNull(customerId))
                .message(message).checkoutUrl(checkoutUrl).createdAt(now).updatedAt(now).build();
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public UUID getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public UUID getStockId() {
        return stockId;
    }

    public UUID getQuoteId() {
        return quoteId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getServiceOrderId() {
        return serviceOrderId;
    }

    public UUID getReferenceId() {
        return switch (type) {
            case LOW_STOCK -> stockId;
            case QUOTE_GENERATED -> quoteId;
            case PAYMENT_CHECKOUT_CREATED, PAYMENT_CONFIRMED, PAYMENT_FAILED -> paymentId;
        };
    }

    public static Notification reconstruct(UUID id, NotificationType type, String message, UUID referenceId, Boolean read, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return reconstruct(id, type, message, referenceId, read, createdAt, updatedAt, null, null, null);
    }

    public static Notification reconstruct(UUID id, NotificationType type, String message, UUID referenceId,
                                           Boolean read, LocalDateTime createdAt, LocalDateTime updatedAt,
                                           UUID serviceOrderId, UUID customerId, String checkoutUrl) {
        Builder builder = builder()
                .id(id)
                .type(type)
                .message(NotificationMessage.fromString(message))
                .read(read)
                .createdAt(createdAt)
                .updatedAt(updatedAt);

        return switch (Objects.requireNonNull(type, "Notification type is required")) {
            case LOW_STOCK -> builder.stockId(referenceId).build();
            case QUOTE_GENERATED -> builder.quoteId(referenceId).serviceOrderId(serviceOrderId)
                    .customerId(customerId).build();
            case PAYMENT_CHECKOUT_CREATED, PAYMENT_CONFIRMED, PAYMENT_FAILED -> builder.paymentId(referenceId)
                    .serviceOrderId(serviceOrderId).customerId(customerId).checkoutUrl(checkoutUrl).build();
        };
    }

    public static Notification create(UUID id, NotificationType type, String message, UUID referenceId) {
        return switch (Objects.requireNonNull(type, "Notification type is required")) {
            case LOW_STOCK -> createForStock(id, referenceId, NotificationMessage.fromString(message));
            case QUOTE_GENERATED -> createForQuote(id, referenceId, NotificationMessage.fromString(message));
            case PAYMENT_CHECKOUT_CREATED, PAYMENT_CONFIRMED, PAYMENT_FAILED ->
                    throw new IllegalArgumentException("Payment notifications require a service order and customer");
        };
    }

    public NotificationMessage getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static class Builder {
        private UUID id;
        private NotificationType type;
        private UUID stockId;
        private UUID quoteId;
        private UUID paymentId;
        private String checkoutUrl;
        private UUID customerId;
        private UUID serviceOrderId;
        private NotificationMessage message;
        private boolean read;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder stockId(UUID stockId) {
            this.stockId = stockId;
            return this;
        }

        public Builder quoteId(UUID quoteId) {
            this.quoteId = quoteId;
            return this;
        }

        public Builder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder paymentId(UUID paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder checkoutUrl(String checkoutUrl) {
            this.checkoutUrl = checkoutUrl;
            return this;
        }

        public Builder serviceOrderId(UUID serviceOrderId) {
            this.serviceOrderId = serviceOrderId;
            return this;
        }

        public Builder message(NotificationMessage message) {
            this.message = message;
            return this;
        }

        public Builder read(boolean read) {
            this.read = read;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }
}
