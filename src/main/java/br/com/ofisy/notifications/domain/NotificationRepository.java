package br.com.ofisy.notifications.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** Notificações do tipo informado, mais recentes primeiro. */
    List<Notification> findAllByType(NotificationType type);

    /** Notificações não lidas do tipo informado, mais recentes primeiro. */
    List<Notification> findUnreadByType(NotificationType type);
}
