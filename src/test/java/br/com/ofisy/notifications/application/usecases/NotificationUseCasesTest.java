package br.com.ofisy.notifications.application.usecases;

import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.domain.NotificationRepository;
import br.com.ofisy.notifications.domain.NotificationType;
import br.com.ofisy.notifications.domain.PaginatedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationUseCasesTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationUseCases useCases;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.create(
                UUID.randomUUID(),
                NotificationType.LOW_STOCK,
                "Test",
                UUID.randomUUID()
        );
    }

    @Test
    void findById() {
        UUID id = testNotification.getId();
        when(repository.findById(id)).thenReturn(Optional.of(testNotification));
        Optional<Notification> result = useCases.findById(id);
        assertTrue(result.isPresent());
        assertEquals(testNotification, result.get());
    }

    @Test
    void markAsRead() {
        UUID id = testNotification.getId();
        when(repository.findById(id)).thenReturn(Optional.of(testNotification));
        when(repository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Notification> result = useCases.markAsRead(id);
        assertTrue(result.isPresent());
        assertTrue(result.get().isRead());
        verify(repository).save(any(Notification.class));
    }

    @Test
    void markAsRead_notFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<Notification> result = useCases.markAsRead(id);
        assertFalse(result.isPresent());
        verify(repository, never()).save(any());
    }

    @Test
    void findStockNotifications() {
        PaginatedResult<Notification> paginated = new PaginatedResult<>(List.of(testNotification), null);
        when(repository.findAllByType(eq(NotificationType.LOW_STOCK), eq(10), any())).thenReturn(paginated);

        PaginatedResult<Notification> result = useCases.findStockNotifications(10, null);
        assertEquals(1, result.getItems().size());
    }

    @Test
    void findUnreadStockNotifications() {
        PaginatedResult<Notification> paginated = new PaginatedResult<>(List.of(testNotification), null);
        when(repository.findUnreadByType(eq(NotificationType.LOW_STOCK), eq(10), any())).thenReturn(paginated);

        PaginatedResult<Notification> result = useCases.findUnreadStockNotifications(10, null);
        assertEquals(1, result.getItems().size());
    }

    @Test
    void findServiceOrderNotifications() {
        PaginatedResult<Notification> paginated = new PaginatedResult<>(List.of(testNotification), null);
        when(repository.findAllByType(eq(NotificationType.QUOTE_GENERATED), eq(10), any())).thenReturn(paginated);

        PaginatedResult<Notification> result = useCases.findServiceOrderNotifications(10, null);
        assertEquals(1, result.getItems().size());
    }

    @Test
    void findUnreadServiceOrderNotifications() {
        PaginatedResult<Notification> paginated = new PaginatedResult<>(List.of(testNotification), null);
        when(repository.findUnreadByType(eq(NotificationType.QUOTE_GENERATED), eq(10), any())).thenReturn(paginated);

        PaginatedResult<Notification> result = useCases.findUnreadServiceOrderNotifications(10, null);
        assertEquals(1, result.getItems().size());
    }

    @Test
    void save() {
        when(repository.save(testNotification)).thenReturn(testNotification);
        Notification result = useCases.save(testNotification);
        assertEquals(testNotification, result);
    }
}


