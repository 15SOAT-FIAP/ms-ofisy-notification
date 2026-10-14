package br.com.ofisy.notifications.adapters.gateways.notification;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {
    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void roundTripPreservesTypeReferenceReadStateAndTimestamps(NotificationType type) {
        var original = Notification.reconstruct(UUID.randomUUID(), type, "Mensagem",
                UUID.randomUUID(), true, LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 2, 11, 0));
        var restored = NotificationMapper.toDomain(NotificationMapper.toEntity(original));
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}
