package br.com.ofisy.notifications.application.notification.findallstock;

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
class FindAllStockNotificationsServiceTest {
    @Mock private NotificationRepository repository;
    @InjectMocks private FindAllStockNotificationsService service;
    private final Notification notification = Notification.create(UUID.randomUUID(),
            NotificationType.LOW_STOCK, "Estoque baixo", UUID.randomUUID());

    @Test
    void forwardsTypeLimitAndCursorAndReturnsPageUnchanged() {
        var cursor = "opaque-continuation-token";
        var page = new PaginatedResult<Notification>(List.of(notification), cursor);
        when(repository.findAllByType(NotificationType.LOW_STOCK, 17, cursor)).thenReturn(page);
        assertThat(service.execute(17, cursor)).isSameAs(page);
        verify(repository).findAllByType(NotificationType.LOW_STOCK, 17, cursor);
    }
}
