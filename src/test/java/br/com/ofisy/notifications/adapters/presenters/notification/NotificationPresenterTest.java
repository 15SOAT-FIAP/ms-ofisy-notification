package br.com.ofisy.notifications.adapters.presenters.notification;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import br.com.ofisy.notifications.shared.pagination.PaginationTokenEncoder;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationPresenterTest {
    @Test
    void preservesResponseFieldsAndPaginationCursor() {
        var notification = Notification.create(UUID.randomUUID(), NotificationType.QUOTE_GENERATED,
                "Orçamento gerado", UUID.randomUUID());
        notification.markAsRead();
        var key = Map.of("id", notification.getId().toString(), "type", "QUOTE_GENERATED");
        var response = NotificationPresenter.presentPage(new PaginatedResult<>(List.of(notification), key));
        var item = response.getItems().getFirst();
        assertThat(item.id()).isEqualTo(notification.getId());
        assertThat(item.referenceId()).isEqualTo(notification.getQuoteId());
        assertThat(item.type()).isEqualTo("QUOTE_GENERATED");
        assertThat(item.message()).isEqualTo(notification.getMessage().getContent());
        assertThat(item.read()).isTrue();
        assertThat(item.createdAt()).isEqualTo(notification.getCreatedAt());
        assertThat(item.updatedAt()).isEqualTo(notification.getUpdatedAt());
        assertThat(PaginationTokenEncoder.decode(response.getNextToken())).isEqualTo(key);
        assertThat(NotificationPresenter.presentPage(new PaginatedResult<>(List.of(), null)).getNextToken()).isNull();
    }
}
