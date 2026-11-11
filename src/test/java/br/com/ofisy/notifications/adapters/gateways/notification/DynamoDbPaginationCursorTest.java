package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.application.notification.exceptions.InvalidPaginationException;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DynamoDbPaginationCursorTest {
    private static final NotificationType TYPE = NotificationType.LOW_STOCK;
    private Map<String, String> key() {
        return Map.of("id", UUID.randomUUID().toString(), "type", TYPE.name(), "createdAt", "2026-01-01T00:00:00");
    }

    @Test
    void roundTripsKeysForEachQueryScope() {
        for (boolean unread : new boolean[] {false, true}) {
            var key = key();
            assertThat(DynamoDbPaginationCursor.decode(DynamoDbPaginationCursor.encode(key, TYPE, unread), TYPE, unread)).isEqualTo(key);
        }
        assertThat(DynamoDbPaginationCursor.encode(null, TYPE, false)).isNull();
        assertThat(DynamoDbPaginationCursor.encode(Map.of(), TYPE, false)).isNull();
        assertThat(DynamoDbPaginationCursor.decode(null, TYPE, false)).isNull();
    }

    @Test
    void rejectsCursorFromAnotherCategoryOrFilter() {
        String cursor = DynamoDbPaginationCursor.encode(key(), TYPE, false);
        assertThatThrownBy(() -> DynamoDbPaginationCursor.decode(cursor, TYPE, true)).isInstanceOf(InvalidPaginationException.class);
        assertThatThrownBy(() -> DynamoDbPaginationCursor.decode(cursor, NotificationType.QUOTE_GENERATED, false)).isInstanceOf(InvalidPaginationException.class);
    }

    @Test
    void rejectsMalformedVersionsKeysAndTypesBeforeCallingDynamoDb() {
        for (String cursor : new String[] {"", " ", "invalid!", "a".repeat(4097)}) {
            assertThatThrownBy(() -> DynamoDbPaginationCursor.decode(cursor, TYPE, false)).isInstanceOf(InvalidPaginationException.class);
        }
        for (String json : new String[] {"[]", "null", "{}", "{\"id\":\"old-token\"}",
                "{\"version\":2,\"scope\":\"LOW_STOCK:all\",\"key\":{}}",
                "{\"version\":1,\"scope\":\"LOW_STOCK:all\",\"key\":{\"bad\":\"x\"}}"}) {
            String cursor = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> DynamoDbPaginationCursor.decode(cursor, TYPE, false)).isInstanceOf(InvalidPaginationException.class);
        }
        for (var badKey : new Map[] {
                Map.of("id", "bad", "type", TYPE.name(), "createdAt", "2026-01-01T00:00:00"),
                Map.of("id", UUID.randomUUID().toString(), "type", TYPE.name(), "createdAt", "bad"),
                Map.of("id", UUID.randomUUID().toString(), "type", "QUOTE_GENERATED", "createdAt", "2026-01-01T00:00:00"),
                Map.of("id", UUID.randomUUID().toString(), "type", TYPE.name(), "createdAt", 123)}) {
            String cursor = DynamoDbPaginationCursor.encode(badKey, TYPE, false);
            assertThatThrownBy(() -> DynamoDbPaginationCursor.decode(cursor, TYPE, false)).isInstanceOf(InvalidPaginationException.class);
        }
    }
}
