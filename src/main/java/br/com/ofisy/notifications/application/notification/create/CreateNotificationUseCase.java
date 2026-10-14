package br.com.ofisy.notifications.application.notification.create;

import br.com.ofisy.notifications.domain.notification.Notification;

public interface CreateNotificationUseCase {
    boolean execute(Notification notification);
}
