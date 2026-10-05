package br.com.ofisy.notifications.adapters.controllers;

import br.com.ofisy.notifications.adapters.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.application.usecases.NotificationUseCases;
import br.com.ofisy.notifications.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final br.com.ofisy.notifications.application.usecases.NotificationUseCases useCases;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN') or @notificationSecurity.isServiceOrderNotification(#id)")
    public ResponseEntity<NotificationResponseDTO> findById(@PathVariable UUID id) {
        return useCases.markAsRead(id)
                .map(NotificationResponseDTO::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN') or @notificationSecurity.isServiceOrderNotification(#id)")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable UUID id) {
        return useCases.markAsRead(id)
                .map(NotificationResponseDTO::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<List<NotificationResponseDTO>> findStockNotifications() {
        List<NotificationResponseDTO> result = useCases.findStockNotifications().stream()
                .map(NotificationResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stock/unread")
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<List<NotificationResponseDTO>> findUnreadStockNotifications() {
        List<NotificationResponseDTO> result = useCases.findUnreadStockNotifications().stream()
                .map(NotificationResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/service-orders")
    public ResponseEntity<List<NotificationResponseDTO>> findServiceOrderNotifications() {
        List<NotificationResponseDTO> result = useCases.findServiceOrderNotifications().stream()
                .map(NotificationResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/service-orders/unread")
    public ResponseEntity<List<NotificationResponseDTO>> findUnreadServiceOrderNotifications() {
        List<NotificationResponseDTO> result = useCases.findUnreadServiceOrderNotifications().stream()
                .map(NotificationResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(result);
    }
}





