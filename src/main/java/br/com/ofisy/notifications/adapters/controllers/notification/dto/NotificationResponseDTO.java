package br.com.ofisy.notifications.adapters.controllers.notification.dto;
import java.time.LocalDateTime;
import java.util.UUID;
/** Core fields plus the additive migration alias referenceId. */
public record NotificationResponseDTO(UUID id,String type,UUID stockId,UUID quoteId,String message,
    Boolean read,LocalDateTime createdAt,LocalDateTime updatedAt,UUID referenceId) {}
