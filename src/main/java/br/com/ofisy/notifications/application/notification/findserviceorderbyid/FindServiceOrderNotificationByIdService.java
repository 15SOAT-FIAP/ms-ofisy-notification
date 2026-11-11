package br.com.ofisy.notifications.application.notification.findserviceorderbyid;

import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class FindServiceOrderNotificationByIdService implements FindServiceOrderNotificationByIdUseCase {

    private final NotificationRepository notificationRepository;

    public FindServiceOrderNotificationByIdService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification execute(UUID id, UUID customerId) {
        return notificationRepository.findById(id)
                .filter(notification -> notification.getType().isServiceOrder())
                .filter(notification -> customerId.equals(notification.getCustomerId()))
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}
