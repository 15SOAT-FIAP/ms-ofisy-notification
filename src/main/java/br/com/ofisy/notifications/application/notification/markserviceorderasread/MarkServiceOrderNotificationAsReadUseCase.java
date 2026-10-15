package br.com.ofisy.notifications.application.notification.markserviceorderasread;
import br.com.ofisy.notifications.domain.notification.Notification;
import java.util.UUID;
public interface MarkServiceOrderNotificationAsReadUseCase {
    Notification execute(MarkAsReadCommand command);
    record MarkAsReadCommand(UUID id) {}
}
