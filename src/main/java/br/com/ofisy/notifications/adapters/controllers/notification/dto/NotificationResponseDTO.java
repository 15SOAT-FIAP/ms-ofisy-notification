package br.com.ofisy.notifications.adapters.controllers.notification.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.UUID;
public record NotificationResponseDTO(
        UUID id,
        String type,
        UUID stockId,
        UUID quoteId,
        String message,
        Boolean read,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID paymentId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String checkoutUrl
) {}
