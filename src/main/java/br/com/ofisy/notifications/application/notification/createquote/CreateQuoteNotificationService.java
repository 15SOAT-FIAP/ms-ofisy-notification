package br.com.ofisy.notifications.application.notification.createquote;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationMessage;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CreateQuoteNotificationService implements CreateQuoteNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public CreateQuoteNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification execute(CreateQuoteCommand command) {
        NotificationMessage message = NotificationMessage.forQuote(
                command.quoteId(),
                command.serviceOrderId(),
                command.totalPrice()
        );
        Notification notification = Notification.createForQuote(command.quoteId(), message);
        return notificationRepository.save(notification);
    }
    @Override
    public boolean executeEvent(UUID eventId, CreateQuoteCommand command) {
        NotificationMessage message = NotificationMessage.forQuote(
                command.quoteId(),
                command.serviceOrderId(),
                command.totalPrice()
        );
        Notification notification = Notification.createForQuote(eventId, command.quoteId(), message);
        return notificationRepository.createIfAbsent(notification);
    }
}
