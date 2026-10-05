package br.com.ofisy.notifications.adapters.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

public class PaginationTokenEncoder {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String encode(Map<String, String> key) {
        if (key == null || key.isEmpty()) return null;
        try {
            String json = mapper.writeValueAsString(key);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static Map<String, String> decode(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(token);
            return mapper.readValue(bytes, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
