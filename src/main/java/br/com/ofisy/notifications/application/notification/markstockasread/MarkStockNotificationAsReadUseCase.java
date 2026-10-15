package br.com.ofisy.notifications.application.notification.markstockasread;
import br.com.ofisy.notifications.domain.notification.Notification;
import java.util.UUID;
public interface MarkStockNotificationAsReadUseCase {
    Notification execute(MarkAsReadCommand command);
    record MarkAsReadCommand(UUID id) {}
}
