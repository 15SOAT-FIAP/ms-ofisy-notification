package br.com.ofisy.notifications.application.notification.markstockasread;
import br.com.ofisy.notifications.domain.notification.*;
import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class MarkStockNotificationAsReadServiceTest {
    private final NotificationRepository repository=mock(NotificationRepository.class);
    private final MarkStockNotificationAsReadService service=new MarkStockNotificationAsReadService(repository);
    private final UUID id=UUID.randomUUID();
    @Test void acceptsExpectedType() {
        var notification=Notification.create(id,NotificationType.LOW_STOCK,"Mensagem",UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);
        assertThat(service.execute(new MarkStockNotificationAsReadUseCase.MarkAsReadCommand(id))).isSameAs(notification);
        assertThat(notification.isRead()).isTrue(); verify(repository).save(notification);
    }
    @Test void missingAndWrongTypeHaveSameNotFoundContractWithoutWrites() {
        var wrong=Notification.create(id,NotificationType.QUOTE_GENERATED,"Mensagem",UUID.randomUUID());
        var before=wrong.getUpdatedAt();
        when(repository.findById(id)).thenReturn(Optional.empty(),Optional.of(wrong));
        for(int i=0;i<2;i++) {
            assertThatThrownBy(()->service.execute(new MarkStockNotificationAsReadUseCase.MarkAsReadCommand(id))).isInstanceOf(NotificationNotFoundException.class)
                    .hasMessage("Notificação não encontrada: "+id);
        }
        assertThat(wrong.isRead()).isFalse();assertThat(wrong.getUpdatedAt()).isEqualTo(before);
        verify(repository,never()).save(any());
    }
}
