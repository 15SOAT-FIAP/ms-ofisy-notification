package br.com.ofisy.notifications.adapters.gateways;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamoDbNotificationEntityTest {

    @Test
    void testEntity() {
        DynamoDbNotificationEntity entity = new DynamoDbNotificationEntity();
        
        UUID id = UUID.randomUUID();
        entity.setId(id.toString());
        assertEquals(id.toString(), entity.getId());

        entity.setType("LOW_STOCK");
        assertEquals("LOW_STOCK", entity.getType());

        entity.setMessage("Hello");
        assertEquals("Hello", entity.getMessage());

        entity.setRead(true);
        assertTrue(entity.getRead());

        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        assertEquals(now, entity.getCreatedAt());

        entity.setUpdatedAt(now);
        assertEquals(now, entity.getUpdatedAt());

        UUID ref = UUID.randomUUID();
        entity.setReferenceId(ref.toString());
        assertEquals(ref.toString(), entity.getReferenceId());
    }
}
