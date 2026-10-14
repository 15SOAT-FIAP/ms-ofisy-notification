package br.com.ofisy.notifications.application.notification.findunreadserviceorder;

import br.com.ofisy.notifications.domain.notification.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindUnreadServiceOrderNotificationsServiceTest {
    @Mock private NotificationRepository repository;
    @InjectMocks private FindUnreadServiceOrderNotificationsService service;
    private final Notification notification = Notification.create(UUID.randomUUID(),
            NotificationType.LOW_STOCK, "Estoque baixo", UUID.randomUUID());

    @Test
    void forwardsTypeLimitAndCursorAndReturnsPageUnchanged() {
        var cursor = Map.of("id", "last-id", "createdAt", "2026-01-01T00:00:00");
        var page = new PaginatedResult<Notification>(List.of(notification), cursor);
        when(repository.findUnreadByType(NotificationType.QUOTE_GENERATED, 17, cursor)).thenReturn(page);
        assertThat(service.execute(17, cursor)).isSameAs(page);
        verify(repository).findUnreadByType(NotificationType.QUOTE_GENERATED, 17, cursor);
    }
}
