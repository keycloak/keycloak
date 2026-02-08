package org.keycloak.services.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.KeycloakSessionUtil;
import org.slf4j.MDC;

/**
 * Filter that adds the X-Request-ID header to HTTP responses.
 *
 * This filter extracts the RequestId that was set by the RequestIdHandler early in the request lifecycle
 * and includes it in the HTTP response headers. This allows clients to correlate requests with server logs
 * and enables better traceability in distributed systems.
 *
 * The RequestId is retrieved from:
 * 1. MDC (Mapped Diagnostic Context) - primary source
 * 2. KeycloakSession attributes - fallback if MDC is not available
 *
 * The filter runs at priority 20, which is after the security headers filter (priority 10)
 * but before most other filters, ensuring the X-Request-ID header is consistently added.
 *
 * @author Keycloak Team
 */
@Provider
@PreMatching
@Priority(20)
public class RequestIdHeaderFilter implements ContainerResponseFilter {

    /**
     * MDC key for the RequestId
     */
    private static final String MDC_REQUEST_ID_KEY = "kc.requestId";

    /**
     * Session attribute key for the RequestId
     */
    private static final String SESSION_REQUEST_ID_KEY = "requestId";

    /**
     * HTTP header name for the RequestId
     */
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        addRequestIdHeader(containerResponseContext);
    }

    /**
     * Adds the X-Request-ID header to the HTTP response if a RequestId is available.
     *
     * The method tries to retrieve the RequestId from multiple sources in order of preference:
     * 1. MDC (most reliable, thread-local context)
     * 2. KeycloakSession attributes (fallback)
     *
     * If a RequestId is found and is not empty, it's added to the response headers.
     * This allows clients to correlate their requests with server-side logging.
     *
     * @param responseContext the HTTP response context where the header will be added
     */
    private void addRequestIdHeader(ContainerResponseContext responseContext) {
        String requestId = null;

        // Try to get RequestId from MDC first (most reliable)
        requestId = MDC.get(MDC_REQUEST_ID_KEY);

        // Fallback to session attribute if MDC is not available
        if (requestId == null) {
            try {
                KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
                if (session != null) {
                    requestId = (String) session.getAttribute(SESSION_REQUEST_ID_KEY);
                }
            } catch (Exception e) {
                // Session might not be available in some contexts, which is acceptable
                // We'll simply not add the header in such cases
            }
        }

        // Add X-Request-ID header if RequestId is available
        if (requestId != null && !requestId.trim().isEmpty()) {
            responseContext.getHeaders().add(REQUEST_ID_HEADER, requestId);
        }
    }
}
