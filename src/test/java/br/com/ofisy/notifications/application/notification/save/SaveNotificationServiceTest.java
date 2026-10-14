package br.com.ofisy.notifications.application.notification.save;

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
class SaveNotificationServiceTest {
    @Mock private NotificationRepository repository;
    @InjectMocks private SaveNotificationService service;
    private final Notification notification = Notification.create(UUID.randomUUID(),
            NotificationType.LOW_STOCK, "Estoque baixo", UUID.randomUUID());

    @Test
    void delegatesSave() {
        when(repository.save(notification)).thenReturn(notification);
        assertThat(service.execute(notification)).isSameAs(notification);
        verify(repository).save(notification);
    }
}
