package br.com.ofisy.notifications.domain.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginatedResultTest {

    @Test
    @DisplayName("Deve expor os itens e a cursor opaco")
    void shouldExposeItemsAndNextCursor() {
        List<String> items = List.of("notificacao-1", "notificacao-2");
        String cursor = "opaque-continuation-token";

        PaginatedResult<String> result = new PaginatedResult<>(items, cursor);

        assertThat(result.getItems()).isEqualTo(items);
        assertThat(result.getNextCursor()).isEqualTo(cursor);
    }

    @Test
    @DisplayName("Deve expor cursor opaco nula na última página")
    void shouldExposeNullNextCursorOnLastPage() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of("notificacao-1"), null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getNextCursor()).isNull();
    }
}
