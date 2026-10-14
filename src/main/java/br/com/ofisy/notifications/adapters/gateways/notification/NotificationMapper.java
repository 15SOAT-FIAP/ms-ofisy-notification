package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import java.util.UUID;

public final class NotificationMapper {
    private NotificationMapper() {}

    public static DynamoDbNotificationEntity toEntity(Notification notification) {
        DynamoDbNotificationEntity entity = new DynamoDbNotificationEntity();
        entity.setId(notification.getId().toString());
        entity.setReferenceId(notification.getReferenceId().toString());
        entity.setType(notification.getType().name());
        entity.setMessage(notification.getMessage().getContent());
        entity.setRead(notification.isRead());
        entity.setCreatedAt(notification.getCreatedAt());
        entity.setUpdatedAt(notification.getUpdatedAt());
        return entity;
    }

    public static Notification toDomain(DynamoDbNotificationEntity entity) {
        return Notification.reconstruct(
                UUID.fromString(entity.getId()),
                NotificationType.valueOf(entity.getType()),
                entity.getMessage(),
                UUID.fromString(entity.getReferenceId()),
                entity.getRead(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
