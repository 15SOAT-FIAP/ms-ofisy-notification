package br.com.ofisy.notifications.domain.notification;

import java.util.List;

public class PaginatedResult<T> {
    private final List<T> items;
    private final String nextCursor;

    public PaginatedResult(List<T> items, String cursor) {
        this.items = items;
        this.nextCursor = cursor;
    }

    public List<T> getItems() {
        return items;
    }

    public String getNextCursor() {
        return nextCursor;
    }
}
