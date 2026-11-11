package br.com.ofisy.notifications.application.notification.markserviceorderasread;

import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class MarkServiceOrderNotificationAsReadService implements MarkServiceOrderNotificationAsReadUseCase {

    private final NotificationRepository notificationRepository;

    public MarkServiceOrderNotificationAsReadService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification execute(MarkAsReadCommand command) {
        return notificationRepository.markServiceOrderAsRead(command.id(), command.customerId())
                .orElseThrow(() -> new NotificationNotFoundException(command.id()));
    }
}
