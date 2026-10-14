package br.com.ofisy.notifications.application.notification.findbyid;

import br.com.ofisy.notifications.domain.notification.Notification;
import java.util.Optional;
import java.util.UUID;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class FindNotificationByIdService implements FindNotificationByIdUseCase {
    private final NotificationRepository repository;

    public FindNotificationByIdService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Notification> execute(UUID id) {
        return repository.findById(id);
    }
}
