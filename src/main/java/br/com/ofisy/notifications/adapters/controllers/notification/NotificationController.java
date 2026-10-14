package br.com.ofisy.notifications.adapters.controllers.notification;

import br.com.ofisy.notifications.adapters.controllers.notification.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.PaginatedResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import br.com.ofisy.notifications.application.notification.findbyid.FindNotificationByIdUseCase;
import br.com.ofisy.notifications.application.notification.markasread.MarkNotificationAsReadUseCase;
import br.com.ofisy.notifications.application.notification.findallstock.FindAllStockNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findunreadstock.FindUnreadStockNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findallserviceorder.FindAllServiceOrderNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findunreadserviceorder.FindUnreadServiceOrderNotificationsUseCase;
import br.com.ofisy.notifications.adapters.presenters.notification.NotificationPresenter;
import br.com.ofisy.notifications.shared.pagination.PaginationTokenEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {
    private final FindNotificationByIdUseCase findNotificationByIdUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    private final FindAllStockNotificationsUseCase findAllStockNotificationsUseCase;
    private final FindUnreadStockNotificationsUseCase findUnreadStockNotificationsUseCase;
    private final FindAllServiceOrderNotificationsUseCase findAllServiceOrderNotificationsUseCase;
    private final FindUnreadServiceOrderNotificationsUseCase findUnreadServiceOrderNotificationsUseCase;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN') or @notificationSecurity.isServiceOrderNotification(#id)")
    public ResponseEntity<NotificationResponseDTO> findById(UUID id) {
        return findNotificationByIdUseCase.execute(id)
                .map(NotificationPresenter::present)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN') or @notificationSecurity.isServiceOrderNotification(#id)")
    public ResponseEntity<NotificationResponseDTO> markAsRead(UUID id) {
        return markNotificationAsReadUseCase.execute(id)
                .map(NotificationPresenter::present)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findStockNotifications(int limit, String nextToken) {
        var page = findAllStockNotificationsUseCase.execute(limit, PaginationTokenEncoder.decode(nextToken));
        return ResponseEntity.ok(NotificationPresenter.presentPage(page));
    }
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadStockNotifications(int limit, String nextToken) {
        var page = findUnreadStockNotificationsUseCase.execute(limit, PaginationTokenEncoder.decode(nextToken));
        return ResponseEntity.ok(NotificationPresenter.presentPage(page));
    }
    @Override
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findServiceOrderNotifications(int limit, String nextToken) {
        var page = findAllServiceOrderNotificationsUseCase.execute(limit, PaginationTokenEncoder.decode(nextToken));
        return ResponseEntity.ok(NotificationPresenter.presentPage(page));
    }
    @Override
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadServiceOrderNotifications(int limit, String nextToken) {
        var page = findUnreadServiceOrderNotificationsUseCase.execute(limit, PaginationTokenEncoder.decode(nextToken));
        return ResponseEntity.ok(NotificationPresenter.presentPage(page));
    }
}
