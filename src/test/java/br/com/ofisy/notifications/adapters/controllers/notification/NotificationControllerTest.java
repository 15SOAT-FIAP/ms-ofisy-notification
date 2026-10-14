package br.com.ofisy.notifications.adapters.controllers.notification;

import br.com.ofisy.notifications.application.notification.findbyid.FindNotificationByIdUseCase;
import br.com.ofisy.notifications.application.notification.markasread.MarkNotificationAsReadUseCase;
import br.com.ofisy.notifications.application.notification.findallstock.FindAllStockNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findunreadstock.FindUnreadStockNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findallserviceorder.FindAllServiceOrderNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findunreadserviceorder.FindUnreadServiceOrderNotificationsUseCase;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationMessage;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FindNotificationByIdUseCase findNotificationByIdUseCase;

    @Mock
    private MarkNotificationAsReadUseCase markNotificationAsReadUseCase;

    @Mock
    private FindAllStockNotificationsUseCase findAllStockNotificationsUseCase;

    @Mock
    private FindUnreadStockNotificationsUseCase findUnreadStockNotificationsUseCase;

    @Mock
    private FindAllServiceOrderNotificationsUseCase findAllServiceOrderNotificationsUseCase;

    @Mock
    private FindUnreadServiceOrderNotificationsUseCase findUnreadServiceOrderNotificationsUseCase;

    @InjectMocks
    private NotificationController controller;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        testNotification = Notification.create(
                UUID.randomUUID(),
                NotificationType.LOW_STOCK,
                "Test Msg",
                UUID.randomUUID()
        );
    }

    @Test
    void findById_Found() throws Exception {
        UUID id = testNotification.getId();
        when(findNotificationByIdUseCase.execute(id)).thenReturn(Optional.of(testNotification));

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("LOW_STOCK"));
    }

    @Test
    void findById_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(findNotificationByIdUseCase.execute(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAsRead_Found() throws Exception {
        UUID id = testNotification.getId();
        when(markNotificationAsReadUseCase.execute(id)).thenReturn(Optional.of(testNotification));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void findStockNotifications() throws Exception {
        PaginatedResult<Notification> result = new PaginatedResult<>(List.of(testNotification), null);
        when(findAllStockNotificationsUseCase.execute(anyInt(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/stock").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(testNotification.getId().toString()));
    }

    @Test
    void findUnreadStockNotifications() throws Exception {
        PaginatedResult<Notification> result = new PaginatedResult<>(List.of(testNotification), null);
        when(findUnreadStockNotificationsUseCase.execute(anyInt(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/stock/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(testNotification.getId().toString()));
    }

    @Test
    void findServiceOrderNotifications() throws Exception {
        PaginatedResult<Notification> result = new PaginatedResult<>(List.of(testNotification), null);
        when(findAllServiceOrderNotificationsUseCase.execute(anyInt(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/service-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(testNotification.getId().toString()));
    }

    @Test
    void findUnreadServiceOrderNotifications() throws Exception {
        PaginatedResult<Notification> result = new PaginatedResult<>(List.of(testNotification), null);
        when(findUnreadServiceOrderNotificationsUseCase.execute(anyInt(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/service-orders/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(testNotification.getId().toString()));
    }
}
