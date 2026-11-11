package br.com.ofisy.notifications.application.notification.markserviceorderasread;
import br.com.ofisy.notifications.domain.notification.*;
import br.com.ofisy.notifications.application.notification.exceptions.NotificationNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class MarkServiceOrderNotificationAsReadServiceTest {
    private final NotificationRepository repository=mock(NotificationRepository.class);
    private final MarkServiceOrderNotificationAsReadService service=new MarkServiceOrderNotificationAsReadService(repository);
    private final UUID id=UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @Test
    void returnsTheResultOfTheConditionalUpdate() {
        var notification = Notification.createForQuote(id, UUID.randomUUID(),
                NotificationMessage.fromString("Mensagem"), UUID.randomUUID(), customerId);
        notification.markAsRead();
        when(repository.markServiceOrderAsRead(id, customerId)).thenReturn(Optional.of(notification));

        assertThat(service.execute(new MarkServiceOrderNotificationAsReadUseCase.MarkAsReadCommand(id, customerId)))
                .isSameAs(notification);
        verify(repository, never()).save(any());
    }

    @Test
    void returnsNotFoundWhenTheConditionalUpdateIsDenied() {
        when(repository.markServiceOrderAsRead(id, customerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(new MarkServiceOrderNotificationAsReadUseCase.MarkAsReadCommand(id, customerId)))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("Notificação não encontrada: " + id);
        verify(repository, never()).save(any());
    }
}
