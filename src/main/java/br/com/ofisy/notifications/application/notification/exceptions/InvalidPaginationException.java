package br.com.ofisy.notifications.application.notification.exceptions;

/** Invalid pagination input, independent of persistence and HTTP. */
public class InvalidPaginationException extends IllegalArgumentException {
    public InvalidPaginationException(String message) {
        super(message);
    }
}
