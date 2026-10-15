package br.com.ofisy.notifications.adapters.controllers.notification;

import br.com.ofisy.notifications.application.notification.findstockbyid.FindStockNotificationByIdService;
import br.com.ofisy.notifications.application.notification.findserviceorderbyid.FindServiceOrderNotificationByIdService;
import br.com.ofisy.notifications.application.notification.markstockasread.MarkStockNotificationAsReadService;
import br.com.ofisy.notifications.application.notification.markserviceorderasread.MarkServiceOrderNotificationAsReadService;
import br.com.ofisy.notifications.application.notification.findallstock.FindAllStockNotificationsService;
import br.com.ofisy.notifications.application.notification.findunreadstock.FindUnreadStockNotificationsService;
import br.com.ofisy.notifications.application.notification.findallserviceorder.FindAllServiceOrderNotificationsService;
import br.com.ofisy.notifications.application.notification.findunreadserviceorder.FindUnreadServiceOrderNotificationsService;
import br.com.ofisy.notifications.config.SecurityConfig;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import br.com.ofisy.notifications.shared.securityfilter.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regression: method security must not replace the MVC controller with a JDK interface proxy. */
@SpringJUnitWebConfig(NotificationWebSecurityContextTest.TestConfiguration.class)
@TestPropertySource(properties = {"app.jwt.secret=architecture-test-secret-with-at-least-32-bytes", "app.jwt.issuer=architecture-test"})
class NotificationWebSecurityContextTest {
    @Autowired private WebApplicationContext context;
    @Autowired private NotificationRepository repository;

    @Test
    void interfaceContractAndClassProxyWorkTogetherWithMethodAuthorization() throws Exception {
        assertThat(AopUtils.isCglibProxy(context.getBean(NotificationApi.class))).isTrue();
        var notification = Notification.create(UUID.randomUUID(), NotificationType.LOW_STOCK, "Estoque baixo", UUID.randomUUID());
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var path = "/api/v1/notifications/stock/" + notification.getId();
        mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId().toString()));
        mvc.perform(get(path).with(user("user").roles("USER"))).andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @Import({NotificationController.class, SecurityConfig.class, JwtAuthFilter.class, FindStockNotificationByIdService.class, FindServiceOrderNotificationByIdService.class, MarkStockNotificationAsReadService.class, MarkServiceOrderNotificationAsReadService.class, FindAllStockNotificationsService.class, FindUnreadStockNotificationsService.class, FindAllServiceOrderNotificationsService.class, FindUnreadServiceOrderNotificationsService.class})
    static class TestConfiguration {
        @Bean
        NotificationRepository repository() {
            return mock(NotificationRepository.class);
        }
    }
}
