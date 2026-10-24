package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamoDbNotificationRepositoryImplTest {

    private DynamoDbTable<DynamoDbNotificationEntity> table;
    private DynamoDbIndex<DynamoDbNotificationEntity> typeIndex;
    private DynamoDbNotificationRepositoryImpl repository;
    private final AtomicReference<QueryEnhancedRequest> lastRequest = new AtomicReference<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        DynamoDbEnhancedClient enhancedClient = mock(DynamoDbEnhancedClient.class);
        table = mock(DynamoDbTable.class);
        typeIndex = mock(DynamoDbIndex.class);

        when(enhancedClient.table(eq("Notifications"), any(TableSchema.class))).thenReturn(table);
        when(table.index("TypeIndex")).thenReturn(typeIndex);

        repository = new DynamoDbNotificationRepositoryImpl(enhancedClient, "Notifications");
    }

    @Test
    void save_shouldPersistMappedEntity() {
        UUID id = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        Notification notification = Notification.create(id, NotificationType.LOW_STOCK, "Estoque baixo", stockId);

        Notification result = repository.save(notification);

        ArgumentCaptor<DynamoDbNotificationEntity> captor = ArgumentCaptor.forClass(DynamoDbNotificationEntity.class);
        verify(table).putItem(captor.capture());
        DynamoDbNotificationEntity entity = captor.getValue();
        assertThat(result).isSameAs(notification);
        assertThat(entity.getId()).isEqualTo(id.toString());
        assertThat(entity.getReferenceId()).isEqualTo(stockId.toString());
        assertThat(entity.getType()).isEqualTo("LOW_STOCK");
        assertThat(entity.getMessage()).isEqualTo("Estoque baixo");
        assertThat(entity.getRead()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createIfAbsentUsesAtomicCondition() {
        var notification = Notification.create(UUID.randomUUID(), NotificationType.LOW_STOCK,
                "Estoque baixo", UUID.randomUUID());
        assertThat(repository.createIfAbsent(notification)).isTrue();
        ArgumentCaptor<PutItemEnhancedRequest<DynamoDbNotificationEntity>> captor =
                ArgumentCaptor.forClass(PutItemEnhancedRequest.class);
        verify(table).putItem(captor.capture());
        assertThat(captor.getValue().conditionExpression().expression()).isEqualTo("attribute_not_exists(#id)");
        assertThat(captor.getValue().conditionExpression().expressionNames()).containsEntry("#id", "id");
        assertThat(captor.getValue().item().getId()).isEqualTo(notification.getId().toString());
    }

    @Test
    void duplicateInsertIsSuccessfulNoOp() {
        doThrow(ConditionalCheckFailedException.builder().message("exists").build())
                .when(table).putItem(any(PutItemEnhancedRequest.class));
        var notification = Notification.create(UUID.randomUUID(), NotificationType.LOW_STOCK,
                "Estoque baixo", UUID.randomUUID());
        assertThat(repository.createIfAbsent(notification)).isFalse();
    }

    @Test
    void infrastructureFailureIsNotTreatedAsDuplicate() {
        doThrow(DynamoDbException.builder().message("unavailable").build())
                .when(table).putItem(any(PutItemEnhancedRequest.class));
        var notification = Notification.create(UUID.randomUUID(), NotificationType.LOW_STOCK,
                "Estoque baixo", UUID.randomUUID());
        assertThatThrownBy(() -> repository.createIfAbsent(notification)).isInstanceOf(DynamoDbException.class);
    }

    @Test
    void findById_shouldReturnDomainWhenFound() {
        DynamoDbNotificationEntity entity = entity(NotificationType.QUOTE_GENERATED);
        when(table.getItem(any(Key.class))).thenReturn(entity);

        Optional<Notification> result = repository.findById(UUID.fromString(entity.getId()));

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThat(result.get().getQuoteId()).isEqualTo(UUID.fromString(entity.getReferenceId()));
    }

    @Test
    void findById_shouldReturnEmptyWhenMissing() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAllByType_shouldMapItemsAndNextKey() {
        DynamoDbNotificationEntity entity = entity(NotificationType.LOW_STOCK);
        Map<String, AttributeValue> lastKey = Map.of("id", AttributeValue.builder().s(entity.getId()).build(), "type", AttributeValue.builder().s(entity.getType()).build(), "createdAt", AttributeValue.builder().s(entity.getCreatedAt().toString()).build());
        stubQuery(List.of(Page.create(List.of(entity), lastKey)));

        PaginatedResult<Notification> result = repository.findAllByType(NotificationType.LOW_STOCK, 10, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStockId()).isEqualTo(UUID.fromString(entity.getReferenceId()));
        assertThat(DynamoDbPaginationCursor.decode(result.getNextCursor(), NotificationType.LOW_STOCK, false)).containsEntry("id", entity.getId());
        assertThat(lastRequest.get().limit()).isEqualTo(10);
        assertThat(lastRequest.get().scanIndexForward()).isFalse();
        assertThat(lastRequest.get().filterExpression()).isNull();
        assertThat(lastRequest.get().exclusiveStartKey()).isNull();
    }

    @Test
    void findUnreadByType_shouldApplyFilterAndStartKey() {
        stubQuery(List.of(Page.create(List.of(), Collections.emptyMap())));

        PaginatedResult<Notification> result = repository.findUnreadByType(
                NotificationType.QUOTE_GENERATED, 5, DynamoDbPaginationCursor.encode(Map.of("id", "11111111-1111-1111-1111-111111111111", "type", "QUOTE_GENERATED", "createdAt", "2026-01-01T00:00:00"), NotificationType.QUOTE_GENERATED, true));

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getNextCursor()).isNull();
        assertThat(lastRequest.get().filterExpression().expression()).isEqualTo("#r = :readVal");
        assertThat(lastRequest.get().exclusiveStartKey()).containsEntry("id", AttributeValue.builder().s("11111111-1111-1111-1111-111111111111").build());
    }

    @Test
    void findAllByType_shouldReturnEmptyWhenNoPages() {
        stubQuery(List.of());

        PaginatedResult<Notification> result = repository.findAllByType(NotificationType.LOW_STOCK, 10, null);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getNextCursor()).isNull();
    }

    @SuppressWarnings("unchecked")
    private void stubQuery(List<Page<DynamoDbNotificationEntity>> pages) {
        when(typeIndex.query(any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<QueryEnhancedRequest.Builder> consumer = invocation.getArgument(0);
            QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder();
            consumer.accept(builder);
            lastRequest.set(builder.build());
            SdkIterable<Page<DynamoDbNotificationEntity>> iterable = pages::iterator;
            return iterable;
        });
    }

    private DynamoDbNotificationEntity entity(NotificationType type) {
        DynamoDbNotificationEntity entity = new DynamoDbNotificationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setReferenceId(UUID.randomUUID().toString());
        entity.setType(type.name());
        entity.setMessage("Mensagem de teste");
        entity.setRead(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}
