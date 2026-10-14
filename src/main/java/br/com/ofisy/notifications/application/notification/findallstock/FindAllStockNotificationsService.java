package br.com.ofisy.notifications.application.notification.findallstock;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class FindAllStockNotificationsService implements FindAllStockNotificationsUseCase {
    private final NotificationRepository repository;

    public FindAllStockNotificationsService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaginatedResult<Notification> execute(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findAllByType(NotificationType.LOW_STOCK, limit, lastEvaluatedKey);
    }
}
