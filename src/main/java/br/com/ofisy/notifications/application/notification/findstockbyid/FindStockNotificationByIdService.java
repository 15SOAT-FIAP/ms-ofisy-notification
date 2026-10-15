package br.com.ofisy.notifications.application.notification.findstockbyid;

import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class FindStockNotificationByIdService implements FindStockNotificationByIdUseCase {

    private final NotificationRepository notificationRepository;

    public FindStockNotificationByIdService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification execute(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        // Trata tipo errado como "nao encontrada" de proposito: nao revela
        // que o ID existe, so que pertence a outra categoria - evita vazar
        // a existencia de notificacoes de OS pra quem so tem acesso a estoque.
        if (notification.getType() != NotificationType.LOW_STOCK) {
            throw new NotificationNotFoundException(id);
        }

        return notification;
    }
}
