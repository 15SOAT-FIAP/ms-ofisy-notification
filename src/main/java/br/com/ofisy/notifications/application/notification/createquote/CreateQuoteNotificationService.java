package br.com.ofisy.notifications.application.notification.createquote;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationMessage;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CreateQuoteNotificationService implements CreateQuoteNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final ServiceOrderCustomerLookup customerLookup;

    public CreateQuoteNotificationService(NotificationRepository notificationRepository, ServiceOrderCustomerLookup customerLookup) {
        this.notificationRepository = notificationRepository;
        this.customerLookup = customerLookup;
    }

    @Override
    public boolean executeEvent(UUID eventId, CreateQuoteCommand command) {
        NotificationMessage message = NotificationMessage.forQuoteSentForApproval(
                command.quoteId(),
                command.serviceOrderId(),
                command.totalPrice()
        );
        UUID customerId = customerLookup.findCustomerId(command.serviceOrderId());
        Notification notification = Notification.createForQuote(eventId, command.quoteId(), message,
                command.serviceOrderId(), customerId);
        return notificationRepository.createIfAbsent(notification);
    }
}
