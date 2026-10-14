package br.com.ofisy.notifications.application.notification.create;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CreateNotificationUseCase {
    boolean execute(Notification notification);
}
