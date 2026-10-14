package br.com.ofisy.notifications.adapters.controllers.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseDTO(
        UUID id,
        String type,
        String message,
        UUID referenceId,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
