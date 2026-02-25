package org.keycloak.testframework.oauth;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Helper class that returns the CIMD metadata provided in the <em>/cimd/metadata</em> context.
 *
 * @author rmartinc
 */
public class CimdProvider implements Closeable {

    public static final String CONTEXT = "/cimd/metadata";

    private final HttpServer httpServer;
    private final OIDCClientRepresentation client;
    private Status responseStatus;
    private String cacheControlHeader;

    public CimdProvider(HttpServer httpServer, OIDCClientRepresentation client) {
        this.httpServer = httpServer;
        this.client = client;
        this.responseStatus = Status.OK;
        httpServer.createContext(CONTEXT, new OIDCClientRepresentationHandler());
    }

    public OIDCClientRepresentation getRepresentation() {
        return client;
    }

    public void setStatus(Status status) {
        this.responseStatus = status;
    }

    public void setCacheControlHeader(String cacheControlHeader) {
        this.cacheControlHeader = cacheControlHeader;
    }

    @Override
    public void close() {
        httpServer.removeContext(CONTEXT);
    }

    private class OIDCClientRepresentationHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                switch (responseStatus) {
                    case OK -> {
                        String metadata = JsonSerialization.writeValueAsString(client);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        if (cacheControlHeader != null) {
                            exchange.getResponseHeaders().add("Cache-Control", cacheControlHeader);
                        }
                        exchange.sendResponseHeaders(Status.OK.getStatusCode(), metadata.length());
                        try (OutputStream out = exchange.getResponseBody()) {
                            out.write(metadata.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    case NOT_MODIFIED -> exchange.sendResponseHeaders(Status.NOT_MODIFIED.getStatusCode(), -1);
                    case NOT_FOUND -> exchange.sendResponseHeaders(Status.NOT_FOUND.getStatusCode(), -1);
                    default -> exchange.sendResponseHeaders(Status.BAD_REQUEST.getStatusCode(), -1);
                }
            }
        }
    }
}
