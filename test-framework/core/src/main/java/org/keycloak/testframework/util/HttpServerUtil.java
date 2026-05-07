package org.keycloak.testframework.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpServerUtil {

    public static void sendResponse(HttpExchange exchange, int statusCode, Map<String, List<String>> headers, byte[] bodyBytes) {

        try {
            long length = bodyBytes != null ? bodyBytes.length : 0;
            exchange.sendResponseHeaders(statusCode, length);
            if (headers != null) {
                Headers responseHeaders = exchange.getResponseHeaders();
                for (var entry : headers.entrySet()) {
                    responseHeaders.put(entry.getKey(), entry.getValue());
                }
            }

            if (bodyBytes != null) {
                try (var os = exchange.getResponseBody()) {
                    os.write(bodyBytes);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            exchange.close();
        }
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, Map<String, List<String>> headers, String body) {
        byte[] bytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : null;
        sendResponse(exchange, statusCode, headers, bytes);
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, Map<String, List<String>> headers) {
        sendResponse(exchange, statusCode, headers, (byte[]) null);
    }
}
