package br.com.ofisy.notifications.domain.notification;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class CoreCreationContractTest {
    @Test void coreFactoryOverloadsKeepTypesReferencesAndMessageValidation() {
        var id=UUID.randomUUID();
        assertThat(Notification.createForStock(id,"Mensagem").getStockId()).isEqualTo(id);
        assertThat(Notification.createForQuote(id,"Mensagem").getQuoteId()).isEqualTo(id);
        assertThat(Notification.createForStock(id,"Mensagem").getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(Notification.createForQuote(id,"Mensagem").getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThatThrownBy(()->Notification.createForStock(id,"")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->Notification.createForQuote(id,"x".repeat(256))).isInstanceOf(RuntimeException.class);
    }
}
