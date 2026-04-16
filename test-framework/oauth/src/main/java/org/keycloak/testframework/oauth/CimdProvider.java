package org.keycloak.testframework.oauth;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private Map<String, Object> additionalProperties;
    private String rawMetadata;
    private boolean serveRequestedClientId;

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

    /**
     * Sets additional properties to be merged into the served client metadata document.
     * Useful for testing documents that contain properties beyond the ones defined in
     * {@link OIDCClientRepresentation}, which the CIMD specification permits.
     */
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    /**
     * Sets a raw response body to be served verbatim instead of the serialized client representation.
     * Useful for testing malformed client metadata documents.
     */
    public void setRawMetadata(String rawMetadata) {
        this.rawMetadata = rawMetadata;
    }

    /**
     * When enabled, the {@code client_id} property of the served client metadata document is the URL that was
     * requested, so that the same representation can be served for several client IDs under the
     * <em>/cimd/metadata</em> context (for example <em>/cimd/metadata/client-1</em>).
     */
    public void setServeRequestedClientId(boolean serveRequestedClientId) {
        this.serveRequestedClientId = serveRequestedClientId;
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
                        String metadata = rawMetadata != null ? rawMetadata : JsonSerialization.writeValueAsString(client);
                        boolean hasAdditionalProperties = additionalProperties != null && !additionalProperties.isEmpty();
                        if (rawMetadata == null && (hasAdditionalProperties || serveRequestedClientId)) {
                            ObjectNode node = (ObjectNode) JsonSerialization.mapper.readTree(metadata);
                            if (hasAdditionalProperties) {
                                additionalProperties.forEach((name, value) -> node.set(name, JsonSerialization.mapper.valueToTree(value)));
                            }
                            if (serveRequestedClientId) {
                                node.put("client_id", URI.create(client.getClientId()).resolve(exchange.getRequestURI().getRawPath()).toString());
                            }
                            metadata = JsonSerialization.writeValueAsString(node);
                        }
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
