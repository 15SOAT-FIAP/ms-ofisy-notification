package br.com.ofisy.notifications.config;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("notificationSecurity")
public class NotificationSecurity {

    private final NotificationRepository notificationRepository;

    public NotificationSecurity(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public boolean isServiceOrderNotification(UUID id) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        return notificationOpt.isPresent() && notificationOpt.get().getType() == NotificationType.QUOTE_GENERATED;
    }
}
