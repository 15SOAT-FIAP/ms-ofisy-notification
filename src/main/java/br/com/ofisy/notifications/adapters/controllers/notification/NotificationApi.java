package br.com.ofisy.notifications.adapters.controllers.notification;

import br.com.ofisy.notifications.adapters.controllers.notification.dto.NotificationResponseDTO;
import br.com.ofisy.notifications.adapters.controllers.notification.dto.PaginatedResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static br.com.ofisy.notifications.config.swagger.SwaggerConfig.BEARER_TOKEN;

/** Core category-specific routes. Wrong type is indistinguishable from missing id (404). */
@Tag(name = "Notificações")
@ApiResponse(responseCode = "200", description = "Operação concluída", useReturnTypeSchema = true)
public interface NotificationApi {
    @Operation(summary = "Consultar uma notificação de estoque", security = @SecurityRequirement(name = BEARER_TOKEN))
    @ApiResponse(responseCode = "401", description = "Autenticação necessária")
    @ApiResponse(responseCode = "403", description = "Perfil sem acesso a notificações de estoque")
    @ApiResponse(responseCode = "404", description = "Notificação de estoque não encontrada")
    @GetMapping("/stock/{id}")
    ResponseEntity<NotificationResponseDTO> findStockById(@PathVariable UUID id);

    @Operation(summary = "Consultar uma notificação de ordem de serviço")
    @ApiResponse(responseCode = "404", description = "Notificação de ordem de serviço não encontrada")
    @GetMapping("/service-orders/{id}")
    ResponseEntity<NotificationResponseDTO> findServiceOrderById(@PathVariable UUID id);

    @Operation(summary = "Marcar uma notificação de estoque como lida", security = @SecurityRequirement(name = BEARER_TOKEN))
    @ApiResponse(responseCode = "401", description = "Autenticação necessária")
    @ApiResponse(responseCode = "403", description = "Perfil sem acesso a notificações de estoque")
    @ApiResponse(responseCode = "404", description = "Notificação de estoque não encontrada")
    @PatchMapping("/stock/{id}/read")
    ResponseEntity<NotificationResponseDTO> markStockAsRead(@PathVariable UUID id);

    @Operation(summary = "Marcar uma notificação de ordem de serviço como lida")
    @ApiResponse(responseCode = "404", description = "Notificação de ordem de serviço não encontrada")
    @PatchMapping("/service-orders/{id}/read")
    ResponseEntity<NotificationResponseDTO> markServiceOrderAsRead(@PathVariable UUID id);

    @Operation(summary = "Listar notificações de estoque", security = @SecurityRequirement(name = BEARER_TOKEN))
    @ApiResponse(responseCode = "400", description = "Limite ou cursor inválido")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária")
    @ApiResponse(responseCode = "403", description = "Perfil sem acesso a notificações de estoque")
    @GetMapping("/stock")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findAllStock(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);

    @Operation(summary = "Listar notificações de estoque não lidas", security = @SecurityRequirement(name = BEARER_TOKEN))
    @ApiResponse(responseCode = "400", description = "Limite ou cursor inválido")
    @ApiResponse(responseCode = "401", description = "Autenticação necessária")
    @ApiResponse(responseCode = "403", description = "Perfil sem acesso a notificações de estoque")
    @GetMapping("/stock/unread")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadStock(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);

    @Operation(summary = "Listar notificações de ordens de serviço")
    @ApiResponse(responseCode = "400", description = "Limite ou cursor inválido")
    @GetMapping("/service-orders")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findAllServiceOrders(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);

    @Operation(summary = "Listar notificações de ordens de serviço não lidas")
    @ApiResponse(responseCode = "400", description = "Limite ou cursor inválido")
    @GetMapping("/service-orders/unread")
    ResponseEntity<PaginatedResponseDTO<NotificationResponseDTO>> findUnreadServiceOrders(@RequestParam(defaultValue="50") int limit, @RequestParam(required=false) String nextToken);
}
