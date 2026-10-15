package br.com.ofisy.notifications.application.notification.markserviceorderasread;

import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class MarkServiceOrderNotificationAsReadService implements MarkServiceOrderNotificationAsReadUseCase {

    private final NotificationRepository notificationRepository;

    public MarkServiceOrderNotificationAsReadService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification execute(MarkAsReadCommand command) {
        Notification notification = notificationRepository.findById(command.id())
                .orElseThrow(() -> new NotificationNotFoundException(command.id()));

        // Espelha o lado de estoque: tipo errado vira "nao encontrada".
        if (notification.getType() != NotificationType.QUOTE_GENERATED) {
            throw new NotificationNotFoundException(command.id());
        }

        notification.markAsRead();
        return notificationRepository.save(notification);
    }
}
