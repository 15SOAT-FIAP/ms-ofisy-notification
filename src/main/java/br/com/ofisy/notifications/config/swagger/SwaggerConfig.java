package br.com.ofisy.notifications.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    public static final String BEARER_TOKEN = "BearerToken";

    @Bean
    public OpenAPI notificationOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Ofisy Notification API").version("1.0.0")
                        .description("Consulta e leitura de notificações de estoque e de ordens de serviço."))
                // URL relativa para o Try it out funcionar pelo Swagger agregado do API Gateway
                .addServersItem(new Server().url("/"))
                .components(new Components().addSecuritySchemes(BEARER_TOKEN, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
