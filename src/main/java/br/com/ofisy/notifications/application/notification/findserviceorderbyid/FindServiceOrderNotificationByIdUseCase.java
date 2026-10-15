package br.com.ofisy.notifications.application.notification.findserviceorderbyid;
import br.com.ofisy.notifications.domain.notification.Notification;
import java.util.UUID;
public interface FindServiceOrderNotificationByIdUseCase {
    Notification execute(UUID id);
    
}
