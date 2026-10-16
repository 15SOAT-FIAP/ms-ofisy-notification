package br.com.ofisy.notifications.adapters.controllers.notification;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.PaginatedResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
/** Core category-specific routes. Wrong type is indistinguishable from missing id (404). */
public interface NotificationApi {
    @GetMapping("/stock/{id}")
    ResponseEntity<NotificationResponseDTO> findStockById(@PathVariable UUID id);
    @GetMapping("/service-orders/{id}")
    ResponseEntity<NotificationResponseDTO> findServiceOrderById(@PathVariable UUID id);
    @PatchMapping("/stock/{id}/read")
    ResponseEntity<NotificationResponseDTO> markStockAsRead(@PathVariable UUID id);
    @PatchMapping("/service-orders/{id}/read")
    ResponseEntity<NotificationResponseDTO> markServiceOrderAsRead(@PathVariable UUID id);
    @GetMapping("/stock")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findAllStock(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);
    @GetMapping("/stock/unread")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadStock(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);
    @GetMapping("/service-orders")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findAllServiceOrders(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);
    @GetMapping("/service-orders/unread")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadServiceOrders(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);
}
