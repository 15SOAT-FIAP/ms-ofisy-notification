package br.com.ofisy.notifications.domain.exceptions;

public class InvalidNotificationMessageException extends RuntimeException {
    public InvalidNotificationMessageException(String message) {
        super(message);
    }
}
