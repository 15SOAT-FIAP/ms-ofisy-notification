package br.com.ofisy.notifications.application.notification.save;

import br.com.ofisy.notifications.domain.notification.Notification;

public interface SaveNotificationUseCase {
    Notification execute(Notification notification);
}
