package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import java.util.UUID;

interface NotificationEventHandler {
    String eventType();

    Result handle(EventEnvelope event);

    record Result(UUID referenceId, boolean created) {}
}
