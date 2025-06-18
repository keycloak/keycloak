package org.keycloak.testframework.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class OAuthCallbackHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] bytes = "<html><body>Happy days</body></html>".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

}
