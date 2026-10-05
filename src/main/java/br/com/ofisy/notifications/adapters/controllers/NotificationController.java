package br.com.ofisy.notifications.adapters.controllers;

import br.com.ofisy.notifications.adapters.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.adapters.dto.PaginatedResponseDTO;
import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.application.usecases.NotificationUseCases;
import br.com.ofisy.notifications.domain.NotificationType;
import br.com.ofisy.notifications.domain.PaginatedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationUseCases useCases;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN') or @notificationSecurity.isServiceOrderNotification(#id)")
    public ResponseEntity<NotificationResponseDTO> findById(@PathVariable UUID id) {
        return useCases.findById(id)
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
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findStockNotifications(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String nextToken) {
        Map<String, String> key = PaginationTokenEncoder.decode(nextToken);
        PaginatedResult<Notification> result = useCases.findStockNotifications(limit, key);
        return ResponseEntity.ok(toPaginatedDto(result));
    }

    @GetMapping("/stock/unread")
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadStockNotifications(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String nextToken) {
        Map<String, String> key = PaginationTokenEncoder.decode(nextToken);
        PaginatedResult<Notification> result = useCases.findUnreadStockNotifications(limit, key);
        return ResponseEntity.ok(toPaginatedDto(result));
    }

    @GetMapping("/service-orders")
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findServiceOrderNotifications(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String nextToken) {
        Map<String, String> key = PaginationTokenEncoder.decode(nextToken);
        PaginatedResult<Notification> result = useCases.findServiceOrderNotifications(limit, key);
        return ResponseEntity.ok(toPaginatedDto(result));
    }

    @GetMapping("/service-orders/unread")
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadServiceOrderNotifications(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String nextToken) {
        Map<String, String> key = PaginationTokenEncoder.decode(nextToken);
        PaginatedResult<Notification> result = useCases.findUnreadServiceOrderNotifications(limit, key);
        return ResponseEntity.ok(toPaginatedDto(result));
    }

    private PaginatedResponseDTO<NotificationResponseDTO> toPaginatedDto(PaginatedResult<Notification> result) {
        List<NotificationResponseDTO> dtos = result.getItems().stream()
                .map(NotificationResponseDTO::fromDomain)
                .collect(Collectors.toList());
        String token = PaginationTokenEncoder.encode(result.getLastEvaluatedKey());
        return new PaginatedResponseDTO<>(dtos, token);
    }
}
