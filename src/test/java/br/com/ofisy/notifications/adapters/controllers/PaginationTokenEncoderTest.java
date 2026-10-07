package br.com.ofisy.notifications.adapters.controllers;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaginationTokenEncoderTest {

    @Test
    void testEncodeDecode() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        String token = PaginationTokenEncoder.encode(map);
        assertNotNull(token);

        Map<String, String> decoded = PaginationTokenEncoder.decode(token);
        assertEquals("value", decoded.get("key"));
    }

    @Test
    void testNull() {
        assertNull(PaginationTokenEncoder.encode(null));
        assertNull(PaginationTokenEncoder.decode(null));
        assertNull(PaginationTokenEncoder.decode(""));
    }
}
