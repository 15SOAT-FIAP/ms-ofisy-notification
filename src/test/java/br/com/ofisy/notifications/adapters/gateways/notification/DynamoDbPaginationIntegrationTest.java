package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.config.DynamoDbConfig;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "DYNAMODB_TEST_ENDPOINT", matches = ".+")
class DynamoDbPaginationIntegrationTest {
    @Test
    void continuesAllFourQueriesWithoutLosingOrRepeatingItemsEvenAcrossEmptyFilteredPages() {
        try (var client = new DynamoDbConfig().localDynamoDbClient(System.getenv("DYNAMODB_TEST_ENDPOINT"), "us-east-1")) {
            var enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
            String name = "PaginationTest-" + UUID.randomUUID();
            var table = enhanced.table(name, TableSchema.fromBean(DynamoDbNotificationEntity.class));
            table.createTable();
            try {
                client.waiter().waitUntilTableExists(r -> r.tableName(name));
                var repository = new DynamoDbNotificationRepositoryImpl(enhanced, name);
                for (var type : NotificationType.values()) {
                    List<UUID> expected = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        UUID id = UUID.randomUUID();
                        var date = LocalDateTime.of(2026, 1, 1, 0, i);
                        repository.save(Notification.reconstruct(id, type, "Notificação " + i, UUID.randomUUID(),
                                i == 4 || i == 2, date, date));
                        expected.addFirst(id);
                    }
                    // Wait for GSI propagation without assuming strongly consistent index reads.
                    var deadline = System.nanoTime() + java.time.Duration.ofSeconds(15).toNanos();
                    while (repository.findAllByType(type, 10, null).getItems().size() != 5 && System.nanoTime() < deadline) {
                        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                    }
                    for (boolean unreadOnly : List.of(false, true)) {
                        String cursor = null;
                        List<UUID> actual = new ArrayList<>();
                        int pages = 0;
                        boolean emptyPageWithContinuation = false;
                        do {
                            var page = unreadOnly ? repository.findUnreadByType(type, 1, cursor)
                                    : repository.findAllByType(type, 1, cursor);
                            actual.addAll(page.getItems().stream().map(Notification::getId).toList());
                            cursor = page.getNextCursor();
                            emptyPageWithContinuation |= page.getItems().isEmpty() && cursor != null;
                            assertThat(++pages).isLessThanOrEqualTo(6);
                        } while (cursor != null);
                        var wanted = unreadOnly ? List.of(expected.get(1), expected.get(3), expected.get(4)) : expected;
                        assertThat(actual).containsExactlyElementsOf(wanted).doesNotHaveDuplicates();
                        if (unreadOnly) assertThat(emptyPageWithContinuation).isTrue();
                    }
                }
            } finally {
                table.deleteTable();
            }
        }
    }
}
