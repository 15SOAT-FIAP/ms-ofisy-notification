package br.com.ofisy.notifications.application.notification.createlowstock;
import br.com.ofisy.notifications.domain.notification.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CreateLowStockNotificationServiceTest {
    @Test void createsUsingDomainFactoryAsInCore() {
        var repository=mock(NotificationRepository.class);
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        var reference=UUID.randomUUID();
        var result=new CreateLowStockNotificationService(repository).execute(new CreateLowStockNotificationUseCase.CreateLowStockCommand(reference,"Radiador",2,10));
        assertThat(result.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(result.getReferenceId()).isEqualTo(reference);assertThat(result.isRead()).isFalse();
        assertThat(result.getId()).isNotNull();verify(repository).save(result);
    }
    @Test void eventCreationUsesAtomicInsertAndPreservesDomainType() {
        var repository=mock(NotificationRepository.class);
        var eventId=UUID.randomUUID();
        when(repository.createIfAbsent(any())).thenReturn(true,false);
        var service=new CreateLowStockNotificationService(repository);
        var command=new CreateLowStockNotificationUseCase.CreateLowStockCommand(UUID.randomUUID(),"Radiador",2,10);
        assertThat(service.executeEvent(eventId,command)).isTrue();
        assertThat(service.executeEvent(eventId,command)).isFalse();
        verify(repository,times(2)).createIfAbsent(argThat(n->n.getId().equals(eventId)&&n.getType()==NotificationType.LOW_STOCK));
        verify(repository,never()).save(any());
    }
}
