package br.com.ofisy.notifications.adapters.controllers.notification.dto;

import java.util.List;

/** nextToken is opaque and is null on the last page. */
public record PaginatedResponseDTO<T>(List<T> items, String nextToken) {}
