package br.com.ofisy.notifications.application.notification.findstockbyid;
import br.com.ofisy.notifications.domain.notification.Notification;
import java.util.UUID;
public interface FindStockNotificationByIdUseCase {
    Notification execute(UUID id);
    
}
