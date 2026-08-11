package com.onemillioncrops.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTest {
    @Test
    void encodesNestedPayloadAndEscapesUnsafeCharacters() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", "crop\n\"drop\"");
        payload.put("values", List.of(1, true, "\u2028"));
        String json = Json.encode(payload);

        assertEquals("{\"message\":\"crop\\n\\\"drop\\\"\",\"values\":[1,true,\"\\u2028\"]}", json);
    }

    @Test
    void rejectsUnknownTypes() {
        assertThrows(IllegalArgumentException.class, () -> Json.encode(new Object()));
    }
}
