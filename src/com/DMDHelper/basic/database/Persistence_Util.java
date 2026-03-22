package com.DMDHelper.basic.database;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Persistence_Util {

    public static String encode_list(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        List<String> encoded = new ArrayList<>();
        for (String value : values) {
            encoded.add(encode_string(value));
        }
        return String.join(",", encoded);
    }

    public static List<String> decode_list(String raw) {
        List<String> values = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return values;
        }

        String[] parts = raw.split(",");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                values.add(decode_string(part));
            }
        }
        return values;
    }

    public static String encode_map(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String encodedKey = encode_string(entry.getKey());
            String encodedValue = encode_string(entry.getValue() == null ? "" : entry.getValue());
            entries.add(encodedKey + ":" + encodedValue);
        }
        return String.join(",", entries);
    }

    public static Map<String, String> decode_map(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            return values;
        }

        String[] entries = raw.split(",");
        for (String entry : entries) {
            if (entry.trim().isEmpty()) {
                continue;
            }
            String[] pair = entry.split(":", 2);
            if (pair.length == 2) {
                values.put(decode_string(pair[0]), decode_string(pair[1]));
            }
        }
        return values;
    }

    private static String encode_string(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode_string(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
