package br.com.ofisy.notifications.application.notification.markasread;

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
class MarkNotificationAsReadServiceTest {
    @Mock private NotificationRepository repository;
    @InjectMocks private MarkNotificationAsReadService service;
    private final Notification notification = Notification.create(UUID.randomUUID(),
            NotificationType.LOW_STOCK, "Estoque baixo", UUID.randomUUID());

    @Test
    void persistsReadState() {
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);
        assertThat(service.execute(notification.getId())).contains(notification);
        assertThat(notification.isRead()).isTrue();
        verify(repository).save(notification);
    }

    @Test
    void missingNotificationDoesNotWrite() {
        when(repository.findById(notification.getId())).thenReturn(Optional.empty());
        assertThat(service.execute(notification.getId())).isEmpty();
        verify(repository, never()).save(any());
    }
}
