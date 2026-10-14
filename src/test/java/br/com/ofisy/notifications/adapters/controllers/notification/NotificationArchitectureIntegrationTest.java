package br.com.ofisy.notifications.adapters.controllers.notification;

import br.com.ofisy.notifications.adapters.gateways.notification.DynamoDbNotificationRepositoryImpl;
import br.com.ofisy.notifications.application.notification.create.CreateNotificationService;
import br.com.ofisy.notifications.application.notification.create.CreateNotificationUseCase;
import br.com.ofisy.notifications.application.notification.findallserviceorder.FindAllServiceOrderNotificationsService;
import br.com.ofisy.notifications.application.notification.findallstock.FindAllStockNotificationsService;
import br.com.ofisy.notifications.application.notification.findbyid.FindNotificationByIdService;
import br.com.ofisy.notifications.application.notification.findunreadserviceorder.FindUnreadServiceOrderNotificationsService;
import br.com.ofisy.notifications.application.notification.findunreadstock.FindUnreadStockNotificationsService;
import br.com.ofisy.notifications.application.notification.markasread.MarkNotificationAsReadService;
import br.com.ofisy.notifications.config.DynamoDbConfig;
import br.com.ofisy.notifications.config.NotificationSecurity;
import br.com.ofisy.notifications.config.SecurityConfig;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import br.com.ofisy.notifications.shared.securityfilter.JwtAuthFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Spring MVC/security/use-case/gateway integration against isolated DynamoDB Local. */
@EnabledIfEnvironmentVariable(named = "DYNAMODB_TEST_ENDPOINT", matches = ".+")
@SpringJUnitWebConfig(NotificationArchitectureIntegrationTest.TestConfiguration.class)
@TestPropertySource(properties = {"app.jwt.secret=architecture-test-secret-with-at-least-32-bytes", "app.jwt.issuer=architecture-test"})
@DirtiesContext
class NotificationArchitectureIntegrationTest {
    private static final String SECRET = "architecture-test-secret-with-at-least-32-bytes";
    private static final String TABLE = "ArchitectureTest-" + UUID.randomUUID();
    @Autowired private WebApplicationContext context;
    @Autowired private CreateNotificationUseCase createNotification;
    @Autowired private NotificationRepository repository;
    @Autowired private DynamoDbClient client;

    @AfterEach
    void cleanup() {
        client.deleteTable(r -> r.tableName(TABLE));
    }

    @Test
    void securedHttpContractTraversesServicesMapperAndDynamoDbWithoutChangingReadOnDuplicate() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var notification = Notification.create(UUID.randomUUID(), NotificationType.LOW_STOCK,
                "Estoque baixo", UUID.randomUUID());
        assertThat(createNotification.execute(notification)).isTrue();
        String path = "/api/v1/notifications/" + notification.getId();
        mvc.perform(get(path)).andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
        mvc.perform(get(path).header("Authorization", "Bearer " + token("wrong-issuer"))).andExpect(status().isUnauthorized());
        String authorization = "Bearer " + token("architecture-test");
        mvc.perform(get(path).header("Authorization", authorization)).andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value(notification.getReferenceId().toString()))
                .andExpect(jsonPath("$.type").value("LOW_STOCK"));
        mvc.perform(patch(path + "/read").header("Authorization", authorization)).andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        var read = repository.findById(notification.getId()).orElseThrow();
        assertThat(createNotification.execute(notification)).isFalse();
        var stored = repository.findById(notification.getId()).orElseThrow();
        assertThat(stored.isRead()).isTrue();
        assertThat(stored.getUpdatedAt()).isEqualTo(read.getUpdatedAt());
        mvc.perform(get("/api/v1/notifications/stock").param("limit", "10").header("Authorization", authorization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(notification.getId().toString()));
    }

    private String token(String issuer) {
        return Jwts.builder().subject("tester").issuer(issuer).claim("roles", List.of("ROLE_ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Configuration
    @EnableWebMvc
    @Import({NotificationController.class, SecurityConfig.class, NotificationSecurity.class, JwtAuthFilter.class,
            CreateNotificationService.class, FindNotificationByIdService.class, MarkNotificationAsReadService.class,
            FindAllStockNotificationsService.class, FindUnreadStockNotificationsService.class,
            FindAllServiceOrderNotificationsService.class, FindUnreadServiceOrderNotificationsService.class})
    static class TestConfiguration {
        @Bean(destroyMethod = "close")
        DynamoDbClient client() {
            return new DynamoDbConfig().localDynamoDbClient(System.getenv("DYNAMODB_TEST_ENDPOINT"), "us-east-1");
        }

        @Bean
        NotificationRepository repository(DynamoDbClient client) {
            client.createTable(r -> r.tableName(TABLE).billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(attribute("id"), attribute("type"), attribute("createdAt"))
                    .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder().indexName("TypeIndex")
                            .keySchema(KeySchemaElement.builder().attributeName("type").keyType(KeyType.HASH).build(),
                                    KeySchemaElement.builder().attributeName("createdAt").keyType(KeyType.RANGE).build())
                            .projection(Projection.builder().projectionType(ProjectionType.ALL).build()).build()));
            return new DynamoDbNotificationRepositoryImpl(DynamoDbEnhancedClient.builder().dynamoDbClient(client).build(), TABLE);
        }

        private AttributeDefinition attribute(String name) {
            return AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.S).build();
        }
    }
}
