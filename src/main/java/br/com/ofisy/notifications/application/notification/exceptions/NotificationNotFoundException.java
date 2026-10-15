package br.com.ofisy.notifications.application.notification.exceptions;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(UUID id) {
        super("Notificação não encontrada: " + id);
    }
}
