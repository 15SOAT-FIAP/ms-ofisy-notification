package br.com.ofisy.notifications.adapters.gateways.notification;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Persistence-only encoding. Preserves the existing HTTP continuation token format. */
final class DynamoDbPaginationCursor {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DynamoDbPaginationCursor() {}

    static String encode(Map<String, String> key) {
        if (key == null || key.isEmpty()) return null;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MAPPER.writeValueAsString(key).getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, String> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return MAPPER.readValue(Base64.getUrlDecoder().decode(cursor), new TypeReference<Map<String, String>>() {});
        } catch (Exception invalid) {
            // Preserve the existing API behavior: an invalid token starts at the first page.
            return null;
        }
    }
}
