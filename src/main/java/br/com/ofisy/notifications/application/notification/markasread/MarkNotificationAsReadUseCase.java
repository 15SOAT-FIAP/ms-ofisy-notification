package br.com.ofisy.notifications.application.notification.markasread;

import br.com.ofisy.notifications.domain.notification.Notification;
import java.util.Optional;
import java.util.UUID;

public interface MarkNotificationAsReadUseCase {
    Optional<Notification> execute(UUID id);
}
