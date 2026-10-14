package br.com.ofisy.notifications.application.notification.create;

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
class CreateNotificationServiceTest {
    @Mock private NotificationRepository repository;
    @InjectMocks private CreateNotificationService service;
    private final Notification notification = Notification.create(UUID.randomUUID(),
            NotificationType.LOW_STOCK, "Estoque baixo", UUID.randomUUID());

    @Test
    void conditionalCreateReturnsDecisionWithoutCallingUpdate() {
        when(repository.createIfAbsent(notification)).thenReturn(true, false);
        assertThat(service.execute(notification)).isTrue();
        assertThat(service.execute(notification)).isFalse();
        verify(repository, never()).save(any());
        verify(repository, never()).findById(any());
    }
}
