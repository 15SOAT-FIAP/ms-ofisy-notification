package br.com.ofisy.notifications.application.notification.findserviceorderbyid;
import br.com.ofisy.notifications.domain.notification.*;
import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FindServiceOrderNotificationByIdServiceTest {
    private final NotificationRepository repository=mock(NotificationRepository.class);
    private final FindServiceOrderNotificationByIdService service=new FindServiceOrderNotificationByIdService(repository);
    private final UUID id=UUID.randomUUID();
    @Test void acceptsExpectedType() {
        var notification=Notification.create(id,NotificationType.QUOTE_GENERATED,"Mensagem",UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(notification));
        
        assertThat(service.execute(id)).isSameAs(notification);
        verify(repository,never()).save(any());
    }
    @Test void missingAndWrongTypeHaveSameNotFoundContractWithoutWrites() {
        var wrong=Notification.create(id,NotificationType.LOW_STOCK,"Mensagem",UUID.randomUUID());
        var before=wrong.getUpdatedAt();
        when(repository.findById(id)).thenReturn(Optional.empty(),Optional.of(wrong));
        for(int i=0;i<2;i++) {
            assertThatThrownBy(()->service.execute(id)).isInstanceOf(NotificationNotFoundException.class)
                    .hasMessage("Notificação não encontrada: "+id);
        }
        assertThat(wrong.isRead()).isFalse();assertThat(wrong.getUpdatedAt()).isEqualTo(before);
        verify(repository,never()).save(any());
    }
}
