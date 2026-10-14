package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.config.DynamoDbConfig;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Real DynamoDB API test. Set DYNAMODB_TEST_ENDPOINT to an isolated DynamoDB Local. */
@EnabledIfEnvironmentVariable(named = "DYNAMODB_TEST_ENDPOINT", matches = ".+")
class DynamoDbNotificationAtomicInsertTest {
    @Test
    void concurrentDuplicateCannotOverwriteReadState() throws Exception {
        try (var client = new DynamoDbConfig().localDynamoDbClient(
                System.getenv("DYNAMODB_TEST_ENDPOINT"), "us-east-1")) {
            String tableName = "AtomicInsertTest-" + UUID.randomUUID();
            client.createTable(r -> r.tableName(tableName).billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build())
                    .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build()));
            try {
                var repository = new DynamoDbNotificationRepositoryImpl(
                        DynamoDbEnhancedClient.builder().dynamoDbClient(client).build(), tableName);
                UUID eventId = UUID.randomUUID();
                var first = Notification.create(eventId, NotificationType.LOW_STOCK, "First delivery", UUID.randomUUID());
                var second = Notification.create(eventId, NotificationType.LOW_STOCK, "Second delivery", UUID.randomUUID());
                var ready = new CountDownLatch(2);
                var start = new CountDownLatch(1);
                try (var executor = Executors.newFixedThreadPool(2)) {
                    var a = executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                        return repository.createIfAbsent(first);
                    });
                    var b = executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                        return repository.createIfAbsent(second);
                    });
                    assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                    start.countDown();
                    assertThat(a.get(20, TimeUnit.SECONDS) ^ b.get(20, TimeUnit.SECONDS)).isTrue();
                }
                var stored = repository.findById(eventId).orElseThrow();
                stored.markAsRead();
                repository.save(stored);
                assertThat(repository.createIfAbsent(second)).isFalse();
                var afterRedelivery = repository.findById(eventId).orElseThrow();
                assertThat(afterRedelivery.isRead()).isTrue();
                assertThat(afterRedelivery.getUpdatedAt()).isEqualTo(stored.getUpdatedAt());
                assertThat(afterRedelivery.getMessage()).isEqualTo(stored.getMessage());
                assertThat(afterRedelivery.getReferenceId()).isEqualTo(stored.getReferenceId());
            } finally {
                client.deleteTable(r -> r.tableName(tableName));
            }
        }
    }
}
