package br.com.ofisy.notifications.config;

import br.com.ofisy.notifications.shared.securityfilter.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withBean(PropertySourcesPlaceholderConfigurer.class, PropertySourcesPlaceholderConfigurer::new)
            .withUserConfiguration(JwtAuthFilter.class);

    @Test
    void configurationWithoutExternalSecretFailsAtStartup() {
        runner.withPropertyValues("APP_JWT_SECRET=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("app.jwt.secret must be configured externally");
        });
    }

    @Test
    void externalSecretAndIssuerCreateFilter() {
        runner.withPropertyValues("APP_JWT_SECRET=external-test-secret-with-more-than-32-bytes",
                "APP_JWT_ISSUER=external-test-issuer").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(JwtAuthFilter.class);
        });
    }
}
