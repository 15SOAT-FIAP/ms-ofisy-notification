package br.com.ofisy.notifications.application.notification.findbyid;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FindNotificationByIdUseCase {
    Optional<Notification> execute(UUID id);
}
