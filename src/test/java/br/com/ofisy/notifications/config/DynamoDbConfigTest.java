package br.com.ofisy.notifications.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class DynamoDbConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DynamoDbConfig.class);

    @Test
    void awsUsesDefaultCredentialsAndRegionalEndpointWithoutLocalOverrides() {
        runner.withPropertyValues("spring.cloud.aws.region.static=sa-east-1",
                "spring.cloud.aws.dynamodb.endpoint=http://localhost:9999").run(context -> {
            assertThat(context).hasSingleBean(DynamoDbClient.class).hasSingleBean(DynamoDbEnhancedClient.class);
            var configuration = context.getBean(DynamoDbClient.class).serviceClientConfiguration();
            assertThat(configuration.credentialsProvider()).isInstanceOf(DefaultCredentialsProvider.class);
            assertThat(configuration.region()).isEqualTo(Region.SA_EAST_1);
            assertThat(configuration.endpointOverride()).isEmpty();
        });
    }

    @Test
    void localProfileUsesExplicitTestCredentialsAndConfigurableEndpoint() {
        runner.withPropertyValues("spring.profiles.active=local",
                "spring.cloud.aws.dynamodb.endpoint=http://localstack:4566").run(context -> {
            assertThat(context).hasSingleBean(DynamoDbClient.class);
            var configuration = context.getBean(DynamoDbClient.class).serviceClientConfiguration();
            assertThat(configuration.credentialsProvider()).isInstanceOf(StaticCredentialsProvider.class);
            var credentials = (StaticCredentialsProvider) configuration.credentialsProvider();
            assertThat(credentials.resolveCredentials().accessKeyId()).isEqualTo("test");
            assertThat(configuration.endpointOverride()).contains(URI.create("http://localstack:4566"));
        });
    }
}
