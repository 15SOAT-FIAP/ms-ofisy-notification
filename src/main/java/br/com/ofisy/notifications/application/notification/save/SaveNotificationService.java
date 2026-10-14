package br.com.ofisy.notifications.application.notification.save;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class SaveNotificationService implements SaveNotificationUseCase {
    private final NotificationRepository repository;

    public SaveNotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification execute(Notification notification) {
        return repository.save(notification);
    }
}
