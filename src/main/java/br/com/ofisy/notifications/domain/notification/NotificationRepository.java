package br.com.ofisy.notifications.domain.notification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** Cursor opaco: interpretação e formato pertencem ao adapter de persistência. */
    /** Notificações do tipo informado, mais recentes primeiro. */
    PaginatedResult<Notification> findAllByType(NotificationType type, int limit, String cursor);

    /** Notificações não lidas do tipo informado, mais recentes primeiro. */
    PaginatedResult<Notification> findUnreadByType(NotificationType type, int limit, String cursor);
}
