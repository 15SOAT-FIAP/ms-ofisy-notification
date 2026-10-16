package br.com.ofisy.notifications.application.notification.findallstock;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
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
    public PaginatedResult<Notification> execute(int limit, String cursor) {
        return repository.findAllByType(NotificationType.LOW_STOCK, limit, cursor);
    }
}
