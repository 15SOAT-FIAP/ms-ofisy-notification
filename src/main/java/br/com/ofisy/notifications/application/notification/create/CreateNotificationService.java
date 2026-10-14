package br.com.ofisy.notifications.application.notification.create;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class CreateNotificationService implements CreateNotificationUseCase {
    private final NotificationRepository repository;

    public CreateNotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean execute(Notification notification) {
        return repository.createIfAbsent(notification);
    }
}
