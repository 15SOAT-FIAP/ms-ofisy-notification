package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

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
        DynamoDbNotificationEntity entity = NotificationMapper.toEntity(notification);
        table.putItem(entity);
        return notification;
    }

    @Override
    public boolean createIfAbsent(Notification notification) {
        var request = PutItemEnhancedRequest.builder(DynamoDbNotificationEntity.class)
                .item(NotificationMapper.toEntity(notification))
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(#id)")
                        .putExpressionName("#id", "id")
                        .build())
                .build();
        try {
            table.putItem(request);
            return true;
        } catch (ConditionalCheckFailedException duplicate) {
            return false;
        }
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        Key key = Key.builder().partitionValue(id.toString()).build();
        DynamoDbNotificationEntity entity = table.getItem(key);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(NotificationMapper.toDomain(entity));
    }

    @Override
    public PaginatedResult<Notification> findAllByType(NotificationType type, int limit, String cursor) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(type.name()).build());
        return executeQuery(queryConditional, null, limit, cursor);
    }

    @Override
    public PaginatedResult<Notification> findUnreadByType(NotificationType type, int limit, String cursor) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(type.name()).build());
        software.amazon.awssdk.enhanced.dynamodb.Expression filterExpression = software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                .expression("#r = :readVal")
                .putExpressionName("#r", "read")
                .putExpressionValue(":readVal", AttributeValue.builder().bool(false).build())
                .build();
                
        return executeQuery(queryConditional, filterExpression, limit, cursor);
    }

    private PaginatedResult<Notification> executeQuery(QueryConditional conditional, software.amazon.awssdk.enhanced.dynamodb.Expression filter, int limit, String cursor) {
        Map<String, String> startKeyMap = DynamoDbPaginationCursor.decode(cursor);
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
                .map(NotificationMapper::toDomain)
                .collect(Collectors.toList());

        Map<String, String> nextKeyMap = null;
        if (page.lastEvaluatedKey() != null && !page.lastEvaluatedKey().isEmpty()) {
            nextKeyMap = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : page.lastEvaluatedKey().entrySet()) {
                nextKeyMap.put(entry.getKey(), entry.getValue().s());
            }
        }

        return new PaginatedResult<>(notifications, DynamoDbPaginationCursor.encode(nextKeyMap));
    }

}
