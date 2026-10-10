package br.com.ofisy.notifications.application.usecases;

import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.domain.NotificationRepository;
import br.com.ofisy.notifications.domain.NotificationType;
import br.com.ofisy.notifications.domain.PaginatedResult;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;

@Service
public class NotificationUseCases {

    private final NotificationRepository repository;

    public NotificationUseCases(NotificationRepository repository) {
        this.repository = repository;
    }

    public Optional<Notification> findById(UUID id) {
        return repository.findById(id);
    }

    public Optional<Notification> markAsRead(UUID id) {
        return repository.findById(id).map(notification -> {
            notification.markAsRead();
            return repository.save(notification);
        });
    }

    public PaginatedResult<Notification> findStockNotifications(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findAllByType(NotificationType.LOW_STOCK, limit, lastEvaluatedKey);
    }

    public PaginatedResult<Notification> findUnreadStockNotifications(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findUnreadByType(NotificationType.LOW_STOCK, limit, lastEvaluatedKey);
    }

    public PaginatedResult<Notification> findServiceOrderNotifications(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findAllByType(NotificationType.QUOTE_GENERATED, limit, lastEvaluatedKey);
    }

    public PaginatedResult<Notification> findUnreadServiceOrderNotifications(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findUnreadByType(NotificationType.QUOTE_GENERATED, limit, lastEvaluatedKey);
    }
    
    public Notification save(Notification notification) {
        return repository.save(notification);
    }
}
