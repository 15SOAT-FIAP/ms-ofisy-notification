package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.application.notification.createquote.ServiceOrderCustomerLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.time.Duration;

@Component
class CoreServiceOrderCustomerGateway implements ServiceOrderCustomerLookup {
    private final RestClient core;

    CoreServiceOrderCustomerGateway(@Value("${app.core.base-url:http://localhost:8080}") String baseUrl,
                                    @Value("${app.core.service-token:}") String token) {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(3));
        requests.setReadTimeout(Duration.ofSeconds(5));
        this.core = RestClient.builder().baseUrl(baseUrl).requestFactory(requests)
                .defaultHeaders(headers -> headers.setBearerAuth(token)).build();
    }

    @Override
    public UUID findCustomerId(UUID serviceOrderId) {
        var response = core.get().uri("/api/v1/service-orders/{id}/customer", serviceOrderId)
                .retrieve().body(CustomerResponse.class);
        if (response == null || response.customerId() == null) {
            throw new IllegalStateException("Core did not return the service order customer");
        }
        return response.customerId();
    }

    private record CustomerResponse(UUID customerId) {}
}
