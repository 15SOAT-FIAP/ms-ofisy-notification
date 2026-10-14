package br.com.ofisy.notifications.domain.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    /** Inserts a new event notification atomically; false means the id already exists. */
    boolean createIfAbsent(Notification notification);

    Optional<Notification> findById(UUID id);

    /** NotificaÃ§Ãµes do tipo informado, mais recentes primeiro. */
    PaginatedResult<Notification> findAllByType(NotificationType type, int limit, Map<String, String> exclusiveStartKey);

    /** NotificaÃ§Ãµes nÃ£o lidas do tipo informado, mais recentes primeiro. */
    PaginatedResult<Notification> findUnreadByType(NotificationType type, int limit, Map<String, String> exclusiveStartKey);
}
