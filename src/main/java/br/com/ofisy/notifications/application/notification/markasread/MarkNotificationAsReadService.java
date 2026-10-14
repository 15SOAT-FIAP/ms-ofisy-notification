package br.com.ofisy.notifications.application.notification.markasread;

import br.com.ofisy.notifications.domain.notification.Notification;
import java.util.Optional;
import java.util.UUID;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class MarkNotificationAsReadService implements MarkNotificationAsReadUseCase {
    private final NotificationRepository repository;

    public MarkNotificationAsReadService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Notification> execute(UUID id) {
        return repository.findById(id).map(notification -> {
            notification.markAsRead();
            return repository.save(notification);
        });
    }
}
