package br.com.ofisy.notifications.adapters.controllers.notification;

import br.com.ofisy.notifications.adapters.controllers.notification.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.PaginatedResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** HTTP contract. Routes and cursor pagination are preserved from the original service. */
public interface NotificationApi {
    @GetMapping("/{id}")
    ResponseEntity<NotificationResponseDTO> findById(@PathVariable UUID id);
    @PatchMapping("/{id}/read")
    ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable UUID id);
    @GetMapping("/stock")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findStockNotifications(@RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String nextToken);
    @GetMapping("/stock/unread")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadStockNotifications(@RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String nextToken);
    @GetMapping("/service-orders")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findServiceOrderNotifications(@RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String nextToken);
    @GetMapping("/service-orders/unread")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadServiceOrderNotifications(@RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String nextToken);
}
