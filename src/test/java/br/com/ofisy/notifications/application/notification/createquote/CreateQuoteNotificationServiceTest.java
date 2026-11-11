package br.com.ofisy.notifications.application.notification.createquote;
import br.com.ofisy.notifications.domain.notification.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CreateQuoteNotificationServiceTest {
    @Test void eventCreationUsesAtomicInsertAndPreservesDomainType() {
        var repository=mock(NotificationRepository.class);
        var eventId=UUID.randomUUID();
        when(repository.createIfAbsent(any())).thenReturn(true,false);
        var customerId = UUID.randomUUID();
        var service = new CreateQuoteNotificationService(repository, order -> customerId);
        var command=new CreateQuoteNotificationUseCase.CreateQuoteCommand(UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("123.45"));
        assertThat(service.executeEvent(eventId,command)).isTrue();
        assertThat(service.executeEvent(eventId,command)).isFalse();
        verify(repository,times(2)).createIfAbsent(argThat(n->n.getId().equals(eventId)&&n.getType()==NotificationType.QUOTE_GENERATED
                &&n.getQuoteId().equals(command.quoteId())
                &&n.getCustomerId().equals(customerId) &&n.getServiceOrderId().equals(command.serviceOrderId())
                &&n.getMessage().getContent().equals("Orçamento #" + command.quoteId()
                + " disponível para aprovação na ordem de serviço '" + command.serviceOrderId() + "'. Valor total: R$ 123.45")));
        verify(repository,never()).save(any());
    }
}
