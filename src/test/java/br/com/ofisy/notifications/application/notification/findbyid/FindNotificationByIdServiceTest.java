package br.com.ofisy.notifications.application.notification.findbyid;

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
class FindNotificationByIdServiceTest {
    @Mock private NotificationRepository repository;
    @InjectMocks private FindNotificationByIdService service;
    private final Notification notification = Notification.create(UUID.randomUUID(),
            NotificationType.LOW_STOCK, "Estoque baixo", UUID.randomUUID());

    @Test
    void returnsExistingNotificationAndAbsence() {
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification), Optional.empty());
        assertThat(service.execute(notification.getId())).contains(notification);
        assertThat(service.execute(notification.getId())).isEmpty();
    }
}
