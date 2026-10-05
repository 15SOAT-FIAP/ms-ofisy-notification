package br.com.ofisy.notifications.adapters.dto;

import java.util.List;

public class PaginatedResponseDTO<T> {
    private List<T> items;
    private String nextToken; // Base64 encoded JSON of lastEvaluatedKey, or just query params

    public PaginatedResponseDTO() {}

    public PaginatedResponseDTO(List<T> items, String nextToken) {
        this.items = items;
        this.nextToken = nextToken;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }
}
