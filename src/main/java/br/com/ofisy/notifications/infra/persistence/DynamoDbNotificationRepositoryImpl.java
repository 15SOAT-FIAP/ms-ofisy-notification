package br.com.ofisy.notifications.infra.persistence;

import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.domain.NotificationRepository;
import br.com.ofisy.notifications.domain.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class DynamoDbNotificationRepositoryImpl implements NotificationRepository {

    private final DynamoDbTable<DynamoDbNotificationEntity> table;
    private final DynamoDbIndex<DynamoDbNotificationEntity> typeIndex;

    public DynamoDbNotificationRepositoryImpl(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${app.aws.dynamodb.table-name:Notifications}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(DynamoDbNotificationEntity.class));
        this.typeIndex = table.index("TypeIndex");
    }

    @Override
    public Notification save(Notification notification) {
        DynamoDbNotificationEntity entity = toEntity(notification);
        table.putItem(entity);
        return notification;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        Key key = Key.builder().partitionValue(id.toString()).build();
        DynamoDbNotificationEntity entity = table.getItem(key);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public List<Notification> findAllByType(NotificationType type) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(type.name()).build());
        return typeIndex.query(r -> r.queryConditional(queryConditional)
                        .scanIndexForward(false)
                        .limit(50))
                .stream()
                .flatMap(page -> page.items().stream())
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findUnreadByType(NotificationType type) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(type.name()).build());
        software.amazon.awssdk.enhanced.dynamodb.Expression filterExpression = software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                .expression("#r = :readVal")
                .putExpressionName("#r", "read")
                .putExpressionValue(":readVal", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(false).build())
                .build();
                
        return typeIndex.query(r -> r.queryConditional(queryConditional)
                        .filterExpression(filterExpression)
                        .scanIndexForward(false)
                        .limit(50))
                .stream()
                .flatMap(page -> page.items().stream())
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private DynamoDbNotificationEntity toEntity(Notification notification) {
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

    private Notification toDomain(DynamoDbNotificationEntity entity) {
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
