package br.com.ofisy.notifications.shared.securityfilter;

import br.com.ofisy.notifications.config.SecurityConfig;
import br.com.ofisy.notifications.config.security.OfisyAccessDeniedHandler;
import br.com.ofisy.notifications.config.security.OfisyAuthenticationEntryPoint;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real security chain and HTTP dispatch; probe endpoints because the REST API is introduced in PR #23. */
@SpringJUnitWebConfig(JwtCustomerRoutesIntegrationTest.WebConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=employee-test-secret-with-at-least-32-bytes",
        "app.jwt.issuer=techchallenge-ofisy-api"
})
class JwtCustomerRoutesIntegrationTest {
    private static final String ROOT = "/api/v1/notifications/service-orders";
    private static final String STOCK = "/api/v1/notifications/stock";
    private static final String EMPLOYEE_SECRET = "employee-test-secret-with-at-least-32-bytes";
    private static final String CUSTOMER_SECRET = "customer-test-secret-with-at-least-32-bytes";

    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/unread", "/123"})
    void gatewayAuthenticatedCustomerReachesPublicGetRoutes(String suffix) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(ROOT + suffix)
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk()).andExpect(content().string("customer-notifications"));
    }

    @Test
    void gatewayAuthenticatedCustomerCanMarkNotificationAsRead() throws Exception {
        mvc.perform(MockMvcRequestBuilders.patch(ROOT + "/123/read")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk()).andExpect(content().string("marked-as-read"));
    }

    @Test
    void customerRouteMatcherRespectsTheServletContextPath() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/notification-service" + ROOT)
                        .contextPath("/notification-service")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void customerRouteMatcherIgnoresQueryParameters() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(ROOT).param("limit", "10").param("nextToken", "opaque")
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void existingAnonymousCustomerRouteContractIsPreserved() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(ROOT)).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders.patch(ROOT + "/123/read")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {STOCK, "/api/v1/notifications/service-orders-admin",
            "/api/v1/notifications/service-orders-evil/123"})
    void customerTokenCannotAuthenticateStockOrSimilarlyNamedRoutes(String path) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(path).header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STOCKMAN"})
    void authorizedEmployeesStillAccessStock(String role) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(STOCK).header("Authorization", "Bearer " + employeeToken(role)))
                .andExpect(status().isOk()).andExpect(content().string("stock-notifications"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATTENDANT", "MECHANIC"})
    void otherEmployeeRolesStillCannotAccessStock(String role) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(STOCK).header("Authorization", "Bearer " + employeeToken(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Usuário sem permissão"))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para acessar este recurso"));
    }

    @Test
    void anonymousAndMalformedEmployeeTokensStillCannotAccessStock() throws Exception {
        assertAuthenticationRequired(MockMvcRequestBuilders.get(STOCK));
        assertAuthenticationRequired(MockMvcRequestBuilders.get(STOCK)
                .header("Authorization", "Bearer invalid-token"));
    }

    private void assertAuthenticationRequired(RequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Usuário não autorizado"))
                .andExpect(jsonPath("$.detail").value("Autenticação necessária para acessar este recurso"));
    }

    private static String customerToken() {
        // Same claims and explicit HS256 as techchallenge-ofisy-auth/internal/jwt/jwt.go.
        // Separate signing key proves that the employee filter does not parse the customer identity.
        return Jwts.builder().issuer("techchallenge-ofisy-auth")
                .subject("c3d4e5f6-a7b8-4012-8def-123456780001")
                .issuedAt(Date.from(Instant.now())).expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(CUSTOMER_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    private static String employeeToken(String role) {
        return Jwts.builder().issuer("techchallenge-ofisy-api").subject("employee@ofisy.com")
                .claim("role", role).expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(EMPLOYEE_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthFilter.class, OfisyAuthenticationEntryPoint.class, OfisyAccessDeniedHandler.class, ProbeController.class})
    static class WebConfig {}

    @RestController
    @TestComponent
    static class ProbeController {
        @GetMapping({ROOT, ROOT + "/unread", ROOT + "/{id}"})
        String customerNotifications() { return "customer-notifications"; }

        @PatchMapping(ROOT + "/{id}/read")
        String markAsRead() { return "marked-as-read"; }

        @GetMapping({STOCK, "/api/v1/notifications/service-orders-admin",
                "/api/v1/notifications/service-orders-evil/{id}"})
        @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
        String stockNotifications() { return "stock-notifications"; }
    }
}
