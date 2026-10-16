package br.com.ofisy.notifications.adapters.gateways.notification;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class DynamoDbPaginationCursorTest {
    @Test
    void preservesLegacyTokenFormatAndUnicode() {
        var key = Map.of("id", "notificação-1", "type", "LOW_STOCK", "createdAt", "2026-01-01T00:00:00");
        assertThat(DynamoDbPaginationCursor.decode(DynamoDbPaginationCursor.encode(key))).isEqualTo(key);
        String legacy = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"id\":\"legacy-id\",\"type\":\"LOW_STOCK\"}".getBytes(StandardCharsets.UTF_8));
        assertThat(DynamoDbPaginationCursor.decode(legacy)).containsEntry("id", "legacy-id").containsEntry("type", "LOW_STOCK");
    }

    @Test
    void preservesNoCursorAndInvalidTokenBehavior() {
        assertThat(DynamoDbPaginationCursor.encode(null)).isNull();
        assertThat(DynamoDbPaginationCursor.encode(Map.of())).isNull();
        assertThat(DynamoDbPaginationCursor.decode(null)).isNull();
        assertThat(DynamoDbPaginationCursor.decode(" ")).isNull();
        assertThat(DynamoDbPaginationCursor.decode("not-a-valid-token!")).isNull();
        assertThat(DynamoDbPaginationCursor.decode(Base64.getUrlEncoder().encodeToString("[]".getBytes(StandardCharsets.UTF_8)))).isNull();
    }
}
