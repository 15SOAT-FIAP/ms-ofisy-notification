package br.com.ofisy.notifications.application.notification.createlowstock;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationMessage;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CreateLowStockNotificationService implements CreateLowStockNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public CreateLowStockNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification execute(CreateLowStockCommand command) {
        NotificationMessage message = NotificationMessage.forLowStock(
                command.productName(),
                command.currentQuantity(),
                command.minThreshold()
        );
        Notification notification = Notification.createForStock(command.stockId(), message);
        return notificationRepository.save(notification);
    }
    @Override
    public boolean executeEvent(UUID eventId, CreateLowStockCommand command) {
        NotificationMessage message = NotificationMessage.forLowStock(
                command.productName(),
                command.currentQuantity(),
                command.minThreshold()
        );
        Notification notification = Notification.createForStock(eventId, command.stockId(), message);
        return notificationRepository.createIfAbsent(notification);
    }
}
