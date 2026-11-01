package br.com.ofisy.notifications.application.notification.createquote;
import br.com.ofisy.notifications.domain.notification.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CreateQuoteNotificationServiceTest {
    @Test void createsUsingDomainFactoryAsInCore() {
        var repository=mock(NotificationRepository.class);
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        var reference=UUID.randomUUID();
        var orderId=UUID.randomUUID();
        var result=new CreateQuoteNotificationService(repository).execute(new CreateQuoteNotificationUseCase.CreateQuoteCommand(reference,orderId,new BigDecimal("123.45")));
        assertThat(result.getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThat(result.getReferenceId()).isEqualTo(reference);assertThat(result.isRead()).isFalse();
        assertThat(result.getMessage().getContent()).isEqualTo("Orçamento #" + reference
                + " gerado para a ordem de serviço '" + orderId + "'. Valor total: R$ 123.45");
        assertThat(result.getId()).isNotNull();verify(repository).save(result);
    }
    @Test void eventCreationUsesAtomicInsertAndPreservesDomainType() {
        var repository=mock(NotificationRepository.class);
        var eventId=UUID.randomUUID();
        when(repository.createIfAbsent(any())).thenReturn(true,false);
        var service=new CreateQuoteNotificationService(repository);
        var command=new CreateQuoteNotificationUseCase.CreateQuoteCommand(UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("123.45"));
        assertThat(service.executeEvent(eventId,command)).isTrue();
        assertThat(service.executeEvent(eventId,command)).isFalse();
        verify(repository,times(2)).createIfAbsent(argThat(n->n.getId().equals(eventId)&&n.getType()==NotificationType.QUOTE_GENERATED
                &&n.getQuoteId().equals(command.quoteId())
                &&n.getMessage().getContent().equals("Orçamento #" + command.quoteId()
                + " disponível para aprovação na ordem de serviço '" + command.serviceOrderId() + "'. Valor total: R$ 123.45")));
        verify(repository,never()).save(any());
    }
}
