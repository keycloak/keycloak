package org.keycloak.services.util;

import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.KeycloakSessionUtil;
import org.slf4j.MDC;

/**
 * Utility class for accessing the current request's unique RequestId.
 *
 * The RequestId is generated or extracted early in the request lifecycle by the RequestIdHandler
 * and is available throughout the request processing via MDC and KeycloakSession attributes.
 *
 * Usage examples:
 * - String requestId = RequestIdUtil.getCurrentRequestId();
 * - if (RequestIdUtil.hasCurrentRequestId()) { ... }
 *
 * @author Keycloak Team
 */
public class RequestIdUtil {

    /**
     * MDC key for the RequestId
     */
    public static final String MDC_REQUEST_ID_KEY = "kc.requestId";

    /**
     * Session attribute key for the RequestId
     */
    public static final String SESSION_REQUEST_ID_KEY = "requestId";

    /**
     * HTTP header name for the RequestId
     */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /**
     * Gets the current request's RequestId.
     *
     * @return the RequestId for the current request, or null if not available
     */
    public static String getCurrentRequestId() {
        // Try MDC first (fastest and most reliable)
        String requestId = MDC.get(MDC_REQUEST_ID_KEY);

        if (requestId != null) {
            return requestId;
        }

        // Fallback to session attribute
        try {
            KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
            if (session != null) {
                requestId = (String) session.getAttribute(SESSION_REQUEST_ID_KEY);
            }
        } catch (Exception e) {
            // Session might not be available, return null
        }

        return requestId;
    }

    /**
     * Checks if a RequestId is available for the current request.
     *
     * @return true if a RequestId is available, false otherwise
     */
    public static boolean hasCurrentRequestId() {
        String requestId = getCurrentRequestId();
        return requestId != null && !requestId.trim().isEmpty();
    }

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private RequestIdUtil() {
        // Utility class
    }
}
