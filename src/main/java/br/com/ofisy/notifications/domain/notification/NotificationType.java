package br.com.ofisy.notifications.domain.notification;

public enum NotificationType {
    LOW_STOCK,
    QUOTE_GENERATED,
    PAYMENT_CHECKOUT_CREATED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED;

    public boolean isServiceOrder() {
        return switch (this) {
            case LOW_STOCK -> false;
            case QUOTE_GENERATED, PAYMENT_CHECKOUT_CREATED, PAYMENT_CONFIRMED, PAYMENT_FAILED -> true;
        };
    }
}
