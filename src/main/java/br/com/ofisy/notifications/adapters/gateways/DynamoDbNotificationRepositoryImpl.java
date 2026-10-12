package br.com.ofisy.notifications.adapters.gateways;

import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.domain.NotificationRepository;
import br.com.ofisy.notifications.domain.NotificationType;
import br.com.ofisy.notifications.domain.PaginatedResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public PaginatedResult<Notification> findAllByType(NotificationType type, int limit, Map<String, String> exclusiveStartKey) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(type.name()).build());
        return executeQuery(queryConditional, null, limit, exclusiveStartKey);
    }

    @Override
    public PaginatedResult<Notification> findUnreadByType(NotificationType type, int limit, Map<String, String> exclusiveStartKey) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(type.name()).build());
        software.amazon.awssdk.enhanced.dynamodb.Expression filterExpression = software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                .expression("#r = :readVal")
                .putExpressionName("#r", "read")
                .putExpressionValue(":readVal", AttributeValue.builder().bool(false).build())
                .build();
                
        return executeQuery(queryConditional, filterExpression, limit, exclusiveStartKey);
    }

    private PaginatedResult<Notification> executeQuery(QueryConditional conditional, software.amazon.awssdk.enhanced.dynamodb.Expression filter, int limit, Map<String, String> startKeyMap) {
        Map<String, AttributeValue> startKey = null;
        if (startKeyMap != null && !startKeyMap.isEmpty()) {
            startKey = new HashMap<>();
            for (Map.Entry<String, String> entry : startKeyMap.entrySet()) {
                startKey.put(entry.getKey(), AttributeValue.builder().s(entry.getValue()).build());
            }
        }

        Map<String, AttributeValue> finalStartKey = startKey;
        software.amazon.awssdk.core.pagination.sync.SdkIterable<Page<DynamoDbNotificationEntity>> pagedResults = typeIndex.query(r -> {
            r.queryConditional(conditional).scanIndexForward(false).limit(limit);
            if (filter != null) {
                r.filterExpression(filter);
            }
            if (finalStartKey != null) {
                r.exclusiveStartKey(finalStartKey);
            }
        });

        var iterator = pagedResults.iterator();
        if (!iterator.hasNext()) {
            return new PaginatedResult<>(List.of(), null);
        }

        Page<DynamoDbNotificationEntity> page = iterator.next();
        List<Notification> notifications = page.items().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        Map<String, String> nextKeyMap = null;
        if (page.lastEvaluatedKey() != null && !page.lastEvaluatedKey().isEmpty()) {
            nextKeyMap = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : page.lastEvaluatedKey().entrySet()) {
                nextKeyMap.put(entry.getKey(), entry.getValue().s());
            }
        }

        return new PaginatedResult<>(notifications, nextKeyMap);
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

