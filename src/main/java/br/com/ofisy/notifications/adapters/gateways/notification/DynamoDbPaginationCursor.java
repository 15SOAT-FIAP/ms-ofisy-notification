package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.application.notification.exceptions.InvalidPaginationException;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

/** Versioned, query-scoped tokens. DynamoDB keys stay in persistence. */
final class DynamoDbPaginationCursor {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private DynamoDbPaginationCursor() {}

    static String encode(Map<String, String> key, NotificationType type, boolean unread) {
        return encode(key, type.name(), unread);
    }

    static String encode(Map<String, String> key, String partition, boolean unread) {
        if (key == null || key.isEmpty()) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MAPPER.writeValueAsString(Map.of("version", 1, "scope", scope(partition, unread), "key", key)).getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, String> decode(String cursor, NotificationType type, boolean unread) {
        return decode(cursor, "type", type.name(), unread);
    }

    static Map<String, String> decode(String cursor, String partitionKey, String partition, boolean unread) {
        if (cursor == null) return null;
        if (cursor.isBlank() || cursor.length() > 4096) throw invalid();
        try {
            var token = MAPPER.readTree(Base64.getUrlDecoder().decode(cursor));
            if (!token.isObject() || token.size() != 3 || !token.path("version").isIntegralNumber()
                    || token.path("version").asInt() != 1
                    || !scope(partition, unread).equals(token.path("scope").asString())) throw invalid();
            var key = token.path("key");
            if (!key.isObject() || key.size() != 3 || !key.path("id").isString()
                    || !key.path(partitionKey).isString() || !key.path("createdAt").isString()) throw invalid();
            String id = key.path("id").asString();
            if (!UUID.fromString(id).toString().equals(id) || !partition.equals(key.path(partitionKey).asString())) throw invalid();
            LocalDateTime.parse(key.path("createdAt").asString());
            return Map.of("id", id, partitionKey, partition, "createdAt", key.path("createdAt").asString());
        } catch (RuntimeException invalid) {
            throw invalid();
        }
    }

    private static String scope(String partition, boolean unread) {
        return partition + (unread ? ":unread" : ":all");
    }

    private static InvalidPaginationException invalid() {
        return new InvalidPaginationException("nextToken inválido para esta consulta");
    }
}
