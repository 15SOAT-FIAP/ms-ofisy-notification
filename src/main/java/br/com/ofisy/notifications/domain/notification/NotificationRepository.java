package br.com.ofisy.notifications.domain.notification;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** Notificações do tipo informado, mais recentes primeiro. */
    PaginatedResult<Notification> findAllByType(NotificationType type, int limit, Map<String, String> exclusiveStartKey);

    /** Notificações não lidas do tipo informado, mais recentes primeiro. */
    PaginatedResult<Notification> findUnreadByType(NotificationType type, int limit, Map<String, String> exclusiveStartKey);
}
