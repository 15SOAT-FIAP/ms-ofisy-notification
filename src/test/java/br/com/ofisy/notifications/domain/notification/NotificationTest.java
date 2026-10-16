package br.com.ofisy.notifications.domain.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class NotificationTest {

    @Test
    @DisplayName("Deve criar notificação de estoque baixo")
    void shouldCreateStockNotification() {
        UUID stockId = UUID.randomUUID();

        Notification notification = Notification.createForStock(UUID.randomUUID(), stockId, NotificationMessage.fromString("Estoque baixo para Radiador"));

        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(notification.getStockId()).isEqualTo(stockId);
        assertThat(notification.getQuoteId()).isNull();
        assertThat(notification.getMessage().getContent()).isEqualTo("Estoque baixo para Radiador");
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve criar notificação de orçamento gerado")
    void shouldCreateQuoteNotification() {
        UUID quoteId = UUID.randomUUID();

        Notification notification = Notification.createForQuote(UUID.randomUUID(), quoteId, NotificationMessage.fromString("Orçamento #123 gerado"));

        assertThat(notification.getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThat(notification.getQuoteId()).isEqualTo(quoteId);
        assertThat(notification.getStockId()).isNull();
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("Deve marcar notificação como lida e atualizar updatedAt")
    void shouldMarkAsRead() {
        Notification notification = Notification.createForStock(UUID.randomUUID(), UUID.randomUUID(), NotificationMessage.fromString("Estoque baixo"));
        LocalDateTime initialUpdatedAt = notification.getUpdatedAt();

        notification.markAsRead();

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);
    }

    @Test
    @DisplayName("Deve reconstruir notificação via Builder")
    void shouldBuildNotification() {
        UUID id = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
        NotificationMessage msg = NotificationMessage.fromString("Orçamento gerado");

        Notification notification = Notification.builder()
                .id(id)
                .type(NotificationType.QUOTE_GENERATED)
                .quoteId(quoteId)
                .message(msg)
                .read(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThat(notification.getQuoteId()).isEqualTo(quoteId);
        assertThat(notification.getMessage()).isEqualTo(msg);
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Deve usar o stockId como referência em notificação de estoque")
    void shouldUseStockIdAsReferenceForStockNotification() {
        UUID stockId = UUID.randomUUID();

        Notification notification = Notification.createForStock(UUID.randomUUID(), stockId, NotificationMessage.fromString("Estoque baixo"));

        assertThat(notification.getReferenceId()).isEqualTo(stockId);
    }

    @Test
    @DisplayName("Deve usar o quoteId como referência em notificação de orçamento")
    void shouldUseQuoteIdAsReferenceForQuoteNotification() {
        UUID quoteId = UUID.randomUUID();

        Notification notification = Notification.createForQuote(UUID.randomUUID(), quoteId, NotificationMessage.fromString("Orçamento gerado"));

        assertThat(notification.getReferenceId()).isEqualTo(quoteId);
    }

    @Test
    @DisplayName("Deve criar notificação de estoque baixo a partir do tipo")
    void shouldCreateLowStockNotificationFromType() {
        UUID id = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        Notification notification = Notification.create(id, NotificationType.LOW_STOCK, "Estoque baixo para Radiador", referenceId);

        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(notification.getStockId()).isEqualTo(referenceId);
        assertThat(notification.getQuoteId()).isNull();
        assertThat(notification.getMessage().getContent()).isEqualTo("Estoque baixo para Radiador");
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("Deve criar notificação de orçamento gerado a partir do tipo")
    void shouldCreateQuoteNotificationFromType() {
        UUID id = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        Notification notification = Notification.create(id, NotificationType.QUOTE_GENERATED, "Orçamento #123 gerado", referenceId);

        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThat(notification.getQuoteId()).isEqualTo(referenceId);
        assertThat(notification.getStockId()).isNull();
        assertThat(notification.getMessage().getContent()).isEqualTo("Orçamento #123 gerado");
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    @DisplayName("Deve reconstruir notificação de estoque baixo")
    void shouldReconstructLowStockNotification() {
        UUID id = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 1, 8, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 2, 9, 45);

        Notification notification = Notification.reconstruct(id, NotificationType.LOW_STOCK, "Estoque baixo para Radiador", referenceId, true, createdAt, updatedAt);

        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(notification.getStockId()).isEqualTo(referenceId);
        assertThat(notification.getQuoteId()).isNull();
        assertThat(notification.getReferenceId()).isEqualTo(referenceId);
        assertThat(notification.getMessage().getContent()).isEqualTo("Estoque baixo para Radiador");
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Deve reconstruir notificação de orçamento gerado")
    void shouldReconstructQuoteNotification() {
        UUID id = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 4, 11, 15);

        Notification notification = Notification.reconstruct(id, NotificationType.QUOTE_GENERATED, "Orçamento #123 gerado", referenceId, false, createdAt, updatedAt);

        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getType()).isEqualTo(NotificationType.QUOTE_GENERATED);
        assertThat(notification.getQuoteId()).isEqualTo(referenceId);
        assertThat(notification.getStockId()).isNull();
        assertThat(notification.getReferenceId()).isEqualTo(referenceId);
        assertThat(notification.getMessage().getContent()).isEqualTo("Orçamento #123 gerado");
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getUpdatedAt()).isEqualTo(updatedAt);
    }
    @Test
    void shouldRejectNullTypeWhenCreating() {
        assertThatNullPointerException().isThrownBy(() ->
                Notification.create(UUID.randomUUID(), null, "Estoque baixo", UUID.randomUUID()))
                .withMessage("Notification type is required");
    }

    @Test
    void shouldRejectNullTypeWhenReconstructing() {
        var now = LocalDateTime.now();
        assertThatNullPointerException().isThrownBy(() ->
                Notification.reconstruct(UUID.randomUUID(), null, "Estoque baixo", UUID.randomUUID(), false, now, now))
                .withMessage("Notification type is required");
    }

}
