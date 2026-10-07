package br.com.ofisy.notifications.domain;

import java.util.List;
import java.util.Map;

public class PaginatedResult<T> {
    private final List<T> items;
    private final Map<String, String> lastEvaluatedKey;

    public PaginatedResult(List<T> items, Map<String, String> lastEvaluatedKey) {
        this.items = items;
        this.lastEvaluatedKey = lastEvaluatedKey;
    }

    public List<T> getItems() {
        return items;
    }

    public Map<String, String> getLastEvaluatedKey() {
        return lastEvaluatedKey;
    }
}
