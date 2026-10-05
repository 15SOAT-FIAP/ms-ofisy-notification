package br.com.ofisy.notifications.application.usecases;

import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.domain.NotificationRepository;
import br.com.ofisy.notifications.domain.NotificationType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public List<Notification> findStockNotifications() {
        return repository.findAllByType(NotificationType.LOW_STOCK);
    }

    public List<Notification> findUnreadStockNotifications() {
        return repository.findUnreadByType(NotificationType.LOW_STOCK);
    }

    public List<Notification> findServiceOrderNotifications() {
        return repository.findAllByType(NotificationType.QUOTE_GENERATED);
    }

    public List<Notification> findUnreadServiceOrderNotifications() {
        return repository.findUnreadByType(NotificationType.QUOTE_GENERATED);
    }
    
    public Notification save(Notification notification) {
        return repository.save(notification);
    }
}
