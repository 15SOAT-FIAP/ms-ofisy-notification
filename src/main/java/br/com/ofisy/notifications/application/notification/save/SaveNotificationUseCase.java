package br.com.ofisy.notifications.application.notification.save;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SaveNotificationUseCase {
    Notification execute(Notification notification);
}
