package br.com.ofisy.notifications.application.notification.findallserviceorder;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class FindAllServiceOrderNotificationsService implements FindAllServiceOrderNotificationsUseCase {
    private final NotificationRepository repository;

    public FindAllServiceOrderNotificationsService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaginatedResult<Notification> execute(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findAllByType(NotificationType.QUOTE_GENERATED, limit, lastEvaluatedKey);
    }
}
