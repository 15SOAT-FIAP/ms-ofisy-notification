package br.com.ofisy.notifications.adapters.presenters.notification;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.PaginatedResponseDTO;
import br.com.ofisy.notifications.shared.pagination.PaginationTokenEncoder;

public final class NotificationPresenter {
    private NotificationPresenter() {}

    public static NotificationResponseDTO present(Notification notification) {
        return new NotificationResponseDTO(notification.getId(), notification.getType().name(),
                notification.getMessage().getContent(), notification.getReferenceId(),
                notification.isRead(), notification.getCreatedAt(), notification.getUpdatedAt());
    }

    public static PaginatedResponseDTO<NotificationResponseDTO> presentPage(PaginatedResult<Notification> page) {
        return new PaginatedResponseDTO<>(page.getItems().stream().map(NotificationPresenter::present).toList(),
                PaginationTokenEncoder.encode(page.getLastEvaluatedKey()));
    }
}
