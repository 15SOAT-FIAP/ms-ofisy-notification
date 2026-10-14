package br.com.ofisy.notifications.application.notification.findunreadstock;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class FindUnreadStockNotificationsService implements FindUnreadStockNotificationsUseCase {
    private final NotificationRepository repository;

    public FindUnreadStockNotificationsService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaginatedResult<Notification> execute(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findUnreadByType(NotificationType.LOW_STOCK, limit, lastEvaluatedKey);
    }
}
