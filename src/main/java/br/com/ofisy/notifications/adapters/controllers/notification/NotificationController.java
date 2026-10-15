package br.com.ofisy.notifications.adapters.controllers.notification;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.PaginatedResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import br.com.ofisy.notifications.application.notification.findstockbyid.FindStockNotificationByIdUseCase;
import br.com.ofisy.notifications.application.notification.findserviceorderbyid.FindServiceOrderNotificationByIdUseCase;
import br.com.ofisy.notifications.application.notification.markstockasread.MarkStockNotificationAsReadUseCase;
import br.com.ofisy.notifications.application.notification.markserviceorderasread.MarkServiceOrderNotificationAsReadUseCase;
import br.com.ofisy.notifications.application.notification.findallstock.FindAllStockNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findunreadstock.FindUnreadStockNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findallserviceorder.FindAllServiceOrderNotificationsUseCase;
import br.com.ofisy.notifications.application.notification.findunreadserviceorder.FindUnreadServiceOrderNotificationsUseCase;
import br.com.ofisy.notifications.adapters.presenters.notification.NotificationPresenter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {
    private final FindStockNotificationByIdUseCase findStockNotificationByIdUseCase;
    private final FindServiceOrderNotificationByIdUseCase findServiceOrderNotificationByIdUseCase;
    private final MarkStockNotificationAsReadUseCase markStockNotificationAsReadUseCase;
    private final MarkServiceOrderNotificationAsReadUseCase markServiceOrderNotificationAsReadUseCase;
    private final FindAllStockNotificationsUseCase findAllStockNotificationsUseCase;
    private final FindUnreadStockNotificationsUseCase findUnreadStockNotificationsUseCase;
    private final FindAllServiceOrderNotificationsUseCase findAllServiceOrderNotificationsUseCase;
    private final FindUnreadServiceOrderNotificationsUseCase findUnreadServiceOrderNotificationsUseCase;
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<NotificationResponseDTO> findStockById(UUID id) {
        return ResponseEntity.ok(NotificationPresenter.present(findStockNotificationByIdUseCase.execute(id)));
    }
    @Override
    public ResponseEntity<NotificationResponseDTO> findServiceOrderById(UUID id) {
        return ResponseEntity.ok(NotificationPresenter.present(findServiceOrderNotificationByIdUseCase.execute(id)));
    }
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<NotificationResponseDTO> markStockAsRead(UUID id) {
        return ResponseEntity.ok(NotificationPresenter.present(markStockNotificationAsReadUseCase.execute(new MarkStockNotificationAsReadUseCase.MarkAsReadCommand(id))));
    }
    @Override
    public ResponseEntity<NotificationResponseDTO> markServiceOrderAsRead(UUID id) {
        return ResponseEntity.ok(NotificationPresenter.present(markServiceOrderNotificationAsReadUseCase.execute(new MarkServiceOrderNotificationAsReadUseCase.MarkAsReadCommand(id))));
    }
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findAllStock(int limit,String nextToken) {
        return ResponseEntity.ok(NotificationPresenter.presentPage(findAllStockNotificationsUseCase.execute(limit, nextToken)));
    }
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STOCKMAN')")
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadStock(int limit,String nextToken) {
        return ResponseEntity.ok(NotificationPresenter.presentPage(findUnreadStockNotificationsUseCase.execute(limit, nextToken)));
    }
    @Override
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findAllServiceOrders(int limit,String nextToken) {
        return ResponseEntity.ok(NotificationPresenter.presentPage(findAllServiceOrderNotificationsUseCase.execute(limit, nextToken)));
    }
    @Override
    public ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadServiceOrders(int limit,String nextToken) {
        return ResponseEntity.ok(NotificationPresenter.presentPage(findUnreadServiceOrderNotificationsUseCase.execute(limit, nextToken)));
    }
}
