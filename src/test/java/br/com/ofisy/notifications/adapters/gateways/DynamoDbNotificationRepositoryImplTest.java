package br.com.ofisy.notifications.adapters.gateways;

import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.domain.NotificationType;
import br.com.ofisy.notifications.domain.PaginatedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
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
        Map<String, AttributeValue> lastKey = Map.of("id", AttributeValue.builder().s(entity.getId()).build());
        stubQuery(List.of(Page.create(List.of(entity), lastKey)));

        PaginatedResult<Notification> result = repository.findAllByType(NotificationType.LOW_STOCK, 10, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getStockId()).isEqualTo(UUID.fromString(entity.getReferenceId()));
        assertThat(result.getLastEvaluatedKey()).containsEntry("id", entity.getId());
        assertThat(lastRequest.get().limit()).isEqualTo(10);
        assertThat(lastRequest.get().scanIndexForward()).isFalse();
        assertThat(lastRequest.get().filterExpression()).isNull();
        assertThat(lastRequest.get().exclusiveStartKey()).isNull();
    }

    @Test
    void findUnreadByType_shouldApplyFilterAndStartKey() {
        stubQuery(List.of(Page.create(List.of(), Collections.emptyMap())));

        PaginatedResult<Notification> result = repository.findUnreadByType(
                NotificationType.QUOTE_GENERATED, 5, Map.of("id", "abc"));

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getLastEvaluatedKey()).isNull();
        assertThat(lastRequest.get().filterExpression().expression()).isEqualTo("#r = :readVal");
        assertThat(lastRequest.get().exclusiveStartKey()).containsEntry("id", AttributeValue.builder().s("abc").build());
    }

    @Test
    void findAllByType_shouldReturnEmptyWhenNoPages() {
        stubQuery(List.of());

        PaginatedResult<Notification> result = repository.findAllByType(NotificationType.LOW_STOCK, 10, Map.of());

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getLastEvaluatedKey()).isNull();
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
