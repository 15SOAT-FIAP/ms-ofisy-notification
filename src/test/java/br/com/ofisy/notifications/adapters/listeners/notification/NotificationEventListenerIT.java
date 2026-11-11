package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.application.notification.markserviceorderasread.MarkServiceOrderNotificationAsReadUseCase;
import br.com.ofisy.notifications.application.notification.markserviceorderasread.MarkServiceOrderNotificationAsReadUseCase.MarkAsReadCommand;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import br.com.ofisy.notifications.application.notification.createquote.ServiceOrderCustomerLookup;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Real broker -> Spring listener -> use case -> DynamoDB; no mocked collaborators. */
@SpringBootTest(properties = {
        "app.jwt.secret=messaging-integration-only-secret-with-more-than-32-bytes",
        "spring.cloud.aws.sqs.listener.poll-timeout=1s"
})
@ActiveProfiles("local")
@Testcontainers
@DirtiesContext
class NotificationEventListenerIT {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration TIMEOUT = Duration.ofSeconds(40);
    private static final String APPROVAL = "QuoteSentForApproval";
    private static final String STOCK = "StockLow";

    @Container
    static final LocalStackContainer AWS = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.9.0"))
            .withServices("sns", "sqs", "dynamodb")
            .withEnv("SQS_ENDPOINT_STRATEGY", "dynamic")
            .withEnv("LOCALSTACK_ENDPOINT", "http://localhost:4566")
            .withEnv("AWS_DEFAULT_REGION", "us-east-1")
            .withEnv("AWS_ACCESS_KEY_ID", "test")
            .withEnv("AWS_SECRET_ACCESS_KEY", "test")
            .withCopyFileToContainer(MountableFile.forHostPath("dev/integration/bootstrap.py", 0755),
                    "/opt/ofisy/bootstrap.py")
            .withCopyToContainer(Transferable.of("#!/bin/sh\nset -eu\npython /opt/ofisy/bootstrap.py\n", 0755),
                    "/etc/localstack/init/ready.d/01-bootstrap.sh")
            .waitingFor(Wait.forHttp("/_localstack/init/ready").forPort(4566)
                    .forResponsePredicate(body -> JSON.readTree(body).path("completed").asBoolean()
                            && body.contains("SUCCESSFUL")));

    private static SqsClient sqs;
    private static SnsClient sns;
    private static String queue;
    private static String dlq;
    private static String billingTopic;
    private static String stockTopic;

    private static final UUID CUSTOMER = UUID.randomUUID();
    @MockitoBean private ServiceOrderCustomerLookup customerLookup;

    @BeforeEach
    void configureCustomerLookup() {
        org.mockito.Mockito.when(customerLookup.findCustomerId(org.mockito.ArgumentMatchers.any())).thenReturn(CUSTOMER);
    }

    @Autowired private NotificationRepository repository;
    @Autowired private MarkServiceOrderNotificationAsReadUseCase markAsRead;

    @DynamicPropertySource
    static void awsProperties(DynamicPropertyRegistry properties) {
        var endpoint = AWS.getEndpoint();
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(AWS.getAccessKey(), AWS.getSecretKey()));
        sqs = SqsClient.builder().endpointOverride(endpoint).region(Region.of(AWS.getRegion()))
                .credentialsProvider(credentials).httpClientBuilder(UrlConnectionHttpClient.builder()).build();
        sns = SnsClient.builder().endpointOverride(endpoint).region(Region.of(AWS.getRegion()))
                .credentialsProvider(credentials).httpClientBuilder(UrlConnectionHttpClient.builder()).build();
        queue = sqs.getQueueUrl(r -> r.queueName("ofisy-notification.fifo")).queueUrl();
        dlq = sqs.getQueueUrl(r -> r.queueName("ofisy-notification-dlq.fifo")).queueUrl();
        billingTopic = topic("ofisy-billing-events.fifo");
        stockTopic = topic("ofisy-stock-events.fifo");
        properties.add("spring.cloud.aws.sqs.endpoint", endpoint::toString);
        properties.add("spring.cloud.aws.dynamodb.endpoint", endpoint::toString);
        properties.add("spring.cloud.aws.region.static", AWS::getRegion);
        properties.add("spring.cloud.aws.credentials.access-key", AWS::getAccessKey);
        properties.add("spring.cloud.aws.credentials.secret-key", AWS::getSecretKey);
        properties.add("app.aws.sqs.notification-queue", () -> "ofisy-notification.fifo");
    }

    @AfterEach
    void allDeliveredMessagesMustBeAcknowledged() {
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(pending(queue)).isZero();
            assertThat(pending(dlq)).isZero();
        });
    }

    @AfterAll
    static void closeClients() {
        if (sqs != null) sqs.close();
        if (sns != null) sns.close();
    }

    @Test
    void shouldCreateNotificationWhenApprovalEventArrivesIntoSqs() {
        var orderId = UUID.randomUUID();
        var quoteId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        send(APPROVAL, eventId, orderId, quotePayload(quoteId));

        Notification created = received(eventId);
        assertThat(created.getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThat(created.getQuoteId()).isEqualTo(quoteId);
        assertThat(created.getStockId()).isNull();
        assertThat(created.isRead()).isFalse();
        assertThat(created.getMessage().getContent()).isEqualTo("Orçamento #" + quoteId
                + " disponível para aprovação na ordem de serviço '" + orderId + "'. Valor total: R$ 123.45");
    }

    @Test
    void shouldCreateStockNotificationWhenStockLowArrivesIntoSqs() {
        var eventId = UUID.randomUUID();
        var stockId = UUID.randomUUID();
        send(STOCK, eventId, stockId, stockPayload(stockId));

        var created = received(eventId);
        assertThat(created.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(created.getStockId()).isEqualTo(stockId);
        assertThat(created.getQuoteId()).isNull();
        assertThat(created.getMessage().getContent())
                .isEqualTo("Estoque baixo para Radiador. Quantidade atual: 2. Mínimo: 10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"QuoteCreated", "StockConsumptionFailed", "QuoteCancelled", "UnknownEvent"})
    void shouldAcknowledgeUnsupportedEventWithoutCreatingNotification(String type) {
        var orderId = UUID.randomUUID();
        var ignoredId = UUID.randomUUID();
        send(type, ignoredId, orderId, quotePayload(UUID.randomUUID()));
        // Same FIFO group: seeing the barrier proves the preceding message was consumed.
        var barrier = UUID.randomUUID();
        send(APPROVAL, barrier, orderId, quotePayload(UUID.randomUUID()));
        received(barrier);

        assertThat(repository.findById(ignoredId)).isEmpty();
        assertThat(sqs.receiveMessage(r -> r.queueUrl(dlq).waitTimeSeconds(1)).messages()).isEmpty();
    }

    @Test
    void shouldRouteOnlyApprovalEventsFromBillingSnsWithoutMessageAttributes() {
        var subscription = sns.listSubscriptionsByTopic(r -> r.topicArn(billingTopic)).subscriptions().getFirst();
        var attrs = sns.getSubscriptionAttributes(r -> r.subscriptionArn(subscription.subscriptionArn())).attributes();
        assertThat(attrs.get("FilterPolicyScope")).isEqualTo("MessageBody");
        assertThat(JSON.readTree(attrs.get("FilterPolicy")))
                .isEqualTo(JSON.valueToTree(Map.of("type", new String[] {APPROVAL})));
        assertThat(attrs.get("RawMessageDelivery")).isEqualTo("true");

        var orderId = UUID.randomUUID();
        var premature = UUID.randomUUID();
        var cancelled = UUID.randomUUID();
        var failed = UUID.randomUUID();
        publish(billingTopic, "QuoteCreated", premature, orderId, quotePayload(UUID.randomUUID()));
        publish(billingTopic, "QuoteCancelled", cancelled, orderId, Map.of());
        publish(stockTopic, "StockConsumptionFailed", failed, orderId, Map.of());
        var approved = UUID.randomUUID();
        publish(billingTopic, APPROVAL, approved, orderId, quotePayload(UUID.randomUUID()));
        received(approved);

        assertThat(repository.findById(premature)).isEmpty();
        assertThat(repository.findById(cancelled)).isEmpty();
        assertThat(repository.findById(failed)).isEmpty();
    }

    @Test
    void shouldNotOverwriteReadStateOrDatesWhenSameEventIsDeliveredAgain() {
        var eventId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var quoteId = UUID.randomUUID();
        send(APPROVAL, eventId, orderId, quotePayload(quoteId));
        received(eventId);
        markAsRead.execute(new MarkAsReadCommand(eventId, CUSTOMER));
        var before = repository.findById(eventId).orElseThrow();

        send(APPROVAL, eventId, orderId, quotePayload(quoteId));
        var barrier = UUID.randomUUID();
        send(APPROVAL, barrier, orderId, quotePayload(UUID.randomUUID()));
        received(barrier);
        var after = repository.findById(eventId).orElseThrow();
        assertThat(after.isRead()).isTrue();
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
        assertThat(after.getQuoteId()).isEqualTo(before.getQuoteId());
        assertThat(after.getMessage()).isEqualTo(before.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {APPROVAL, STOCK})
    void shouldRetryInvalidPayloadMoveItToDlqAndProcessRepairedEvent(String type) {
        var eventId = UUID.randomUUID();
        var groupId = UUID.randomUUID();
        var referenceId = UUID.randomUUID();
        var invalid = type.equals(APPROVAL) ? Map.<String, Object>of("quoteId", referenceId.toString())
                : Map.<String, Object>of("stockId", referenceId.toString(), "productName", "Radiador", "minThreshold", 10);
        send(type, eventId, groupId, invalid);
        var healthy = UUID.randomUUID();
        send(APPROVAL, healthy, UUID.randomUUID(), quotePayload(UUID.randomUUID()));
        received(healthy); // An invalid message must not block independent groups.

        var failed = await().atMost(TIMEOUT).until(() ->
                sqs.receiveMessage(r -> r.queueUrl(dlq).waitTimeSeconds(1)).messages(), messages -> !messages.isEmpty()).getFirst();
        assertThat(JSON.readTree(failed.body()).path("eventId").asString()).isEqualTo(eventId.toString());
        assertThat(repository.findById(eventId)).isEmpty();

        send(type, eventId, groupId, type.equals(APPROVAL) ? quotePayload(referenceId) : stockPayload(referenceId));
        var repaired = received(eventId);
        assertThat(repaired.getReferenceId()).isEqualTo(referenceId);
        sqs.deleteMessage(r -> r.queueUrl(dlq).receiptHandle(failed.receiptHandle()));
    }

    private Notification received(UUID id) {
        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(repository.findById(id)).isPresent());
        return repository.findById(id).orElseThrow();
    }

    private static void send(String type, UUID id, UUID groupId, Map<String, Object> payload) {
        sqs.sendMessage(r -> r.queueUrl(queue).messageBody(body(type, id, groupId, payload))
                .messageGroupId(groupId.toString()).messageDeduplicationId(UUID.randomUUID().toString()));
    }

    private static void publish(String topic, String type, UUID id, UUID groupId, Map<String, Object> payload) {
        sns.publish(r -> r.topicArn(topic).message(body(type, id, groupId, payload))
                .messageGroupId(groupId.toString()).messageDeduplicationId(UUID.randomUUID().toString()));
    }

    private static String body(String type, UUID id, UUID groupId, Map<String, Object> payload) {
        var event = new HashMap<String, Object>();
        event.put("eventId", id.toString());
        event.put("type", type);
        event.put("serviceOrderId", type.equals(STOCK) ? null : groupId.toString());
        event.put("occurredAt", "2026-11-01T18:00:00Z");
        event.put("payload", payload);
        return JSON.writeValueAsString(event);
    }

    private static Map<String, Object> quotePayload(UUID id) {
        return Map.of("quoteId", id.toString(), "totalPrice", new BigDecimal("123.45"));
    }

    private static Map<String, Object> stockPayload(UUID id) {
        return Map.of("stockId", id.toString(), "productName", "Radiador", "currentQuantity", 2, "minThreshold", 10);
    }

    private static String topic(String name) {
        return sns.listTopics().topics().stream().map(t -> t.topicArn())
                .filter(arn -> arn.endsWith(":" + name)).findFirst().orElseThrow();
    }

    private static int pending(String url) {
        return sqs.getQueueAttributes(r -> r.queueUrl(url).attributeNames(
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)).attributes().values()
                .stream().mapToInt(Integer::parseInt).sum();
    }
}
