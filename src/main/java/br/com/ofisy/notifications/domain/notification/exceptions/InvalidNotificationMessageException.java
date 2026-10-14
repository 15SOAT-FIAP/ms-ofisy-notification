package br.com.ofisy.notifications.domain.notification.exceptions;

public class InvalidNotificationMessageException extends RuntimeException {
    public InvalidNotificationMessageException(String message) {
        super(message);
    }
}
