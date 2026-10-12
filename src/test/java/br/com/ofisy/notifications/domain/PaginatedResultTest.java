package br.com.ofisy.notifications.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaginatedResultTest {

    @Test
    @DisplayName("Deve expor os itens e a chave de paginação")
    void shouldExposeItemsAndLastEvaluatedKey() {
        List<String> items = List.of("notificacao-1", "notificacao-2");
        Map<String, String> lastEvaluatedKey = Map.of("id", "notificacao-2");

        PaginatedResult<String> result = new PaginatedResult<>(items, lastEvaluatedKey);

        assertThat(result.getItems()).isEqualTo(items);
        assertThat(result.getLastEvaluatedKey()).isEqualTo(lastEvaluatedKey);
    }

    @Test
    @DisplayName("Deve expor chave de paginação nula na última página")
    void shouldExposeNullLastEvaluatedKeyOnLastPage() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of("notificacao-1"), null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getLastEvaluatedKey()).isNull();
    }
}
