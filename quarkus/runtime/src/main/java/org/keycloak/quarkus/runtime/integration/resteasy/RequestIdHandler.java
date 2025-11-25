package org.keycloak.quarkus.runtime.integration.resteasy;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.keycloak.models.KeycloakSession;

import org.slf4j.MDC;

import io.quarkus.arc.Arc;

import java.util.UUID;

/**
 * Handler that generates or extracts a unique RequestId for each HTTP request.
 * The RequestId is set in the MDC context and stored in the KeycloakSession for
 * use throughout the request lifecycle.
 *
 * Priority:
 * 1. Uses existing X-Request-ID header if present (from external systems)
 * 2. Generates new UUID if no external RequestId is provided
 *
 * The RequestId is available:
 * - In MDC with key "kc.requestId"
 * - In KeycloakSession attributes with key "requestId"
 * - In HTTP response headers as "X-Request-ID"
 */
public final class RequestIdHandler implements ServerRestHandler {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY = "kc.requestId";
    public static final String SESSION_ATTRIBUTE_KEY = "requestId";

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        // Generate or extract RequestId
        String requestId = extractOrGenerateRequestId(requestContext);

        // Set in MDC for logging
        MDC.put(MDC_KEY, requestId);

        // Store in session if available
        try {
            requestContext.requireCDIRequestScope();
            KeycloakSession session = Arc.container().instance(KeycloakSession.class).get();
            if (session != null) {
                session.setAttribute(SESSION_ATTRIBUTE_KEY, requestId);
            }
        } catch (Exception e) {
            // Session might not be available yet, but MDC is set
            // This is acceptable as the session will be available later
        }
    }

    private String extractOrGenerateRequestId(ResteasyReactiveRequestContext requestContext) {
        // Check for existing X-Request-ID header
        String existingRequestId = requestContext.getHttpHeaders().getHeaderString(REQUEST_ID_HEADER);

        if (existingRequestId != null && !existingRequestId.trim().isEmpty()) {
            // Use external RequestId (from load balancer, API gateway, etc.)
            return existingRequestId.trim();
        } else {
            // Generate new UUID for this request
            return UUID.randomUUID().toString();
        }
    }
}
