package org.keycloak.testframework.oauth;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 *
 * @author rmartinc
 */
public class SectorIdentifierRedirectUrisProvider implements Closeable {

    public static final String CONTEXT = "/sector-identifier-redirect-uris";

    private final HttpServer httpServer;
    private List<String> sectorIdentifierRedirectUris;

    public SectorIdentifierRedirectUrisProvider(HttpServer httpServer, String[] sectorIdentifierRedirectUris) {
        this.httpServer = httpServer;
        this.sectorIdentifierRedirectUris = sectorIdentifierRedirectUris != null ? List.of(sectorIdentifierRedirectUris) : List.of();
        this.httpServer.createContext(CONTEXT, new SectorIdentifierRedirectUrisHandler());
    }

    public void setSectorIdentifierRedirectUris(List<String> uris) {
        this.sectorIdentifierRedirectUris = uris != null ? uris : List.of();
    }

    public List<String> getSectorIdentifierRedirectUris() {
        return sectorIdentifierRedirectUris;
    }

    public String getUri() {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort() + CONTEXT;
    }

    @Override
    public void close() {
        httpServer.removeContext(CONTEXT);
    }

    private class SectorIdentifierRedirectUrisHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String metadata = JsonSerialization.writeValueAsString(sectorIdentifierRedirectUris);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(Response.Status.OK.getStatusCode(), metadata.length());
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(metadata.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
