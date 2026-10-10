package br.com.ofisy.notifications.adapters.dto;

import br.com.ofisy.notifications.domain.Notification;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseDTO(
        UUID id,
        String type,
        String message,
        UUID referenceId,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NotificationResponseDTO fromDomain(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getType().name(),
                notification.getMessage().getContent(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
