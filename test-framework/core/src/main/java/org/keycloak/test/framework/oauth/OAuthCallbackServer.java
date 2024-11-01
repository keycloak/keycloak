package org.keycloak.test.framework.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

class OAuthCallbackServer {

    private final HttpServer httpServer;
    private final OAuthCallbackHandler callbackHandler;
    private final URI redirectionUri;

    public OAuthCallbackServer() {
        this.callbackHandler = new OAuthCallbackHandler();

        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/callback/oauth", callbackHandler);
            httpServer.start();
            redirectionUri = new URI("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/callback/oauth");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public URI getRedirectionUri() {
        return redirectionUri;
    }

    public List<URI> getCallbacks() {
        return callbackHandler.callbacks;
    }

    public void close() {
        httpServer.stop(0);
    }

    static class OAuthCallbackHandler implements HttpHandler {

        private List<URI> callbacks = new LinkedList<>();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            callbacks.add(exchange.getRequestURI());
            byte[] happydays = new String("<html><body>Happy days</body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, happydays.length);
            exchange.getResponseBody().write(happydays);
            exchange.getResponseBody().close();
        }
    }

}
