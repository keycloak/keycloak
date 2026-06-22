package org.keycloak.quarkus.runtime.services;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

public class ContentTypeExceptionFilter implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(ContentTypeExceptionFilter.class);
    private static final String MALFORMED_CONTENT_TYPE_RESPONSE =
            "{\"error\":\"invalid_request\",\"error_description\":\"Invalid Content-Type header\"}";

    @Override
    public void handle(RoutingContext routingContext) {
        String contentType = routingContext.request().getHeader("Content-Type");

        if (contentType != null && !contentType.isBlank() && !isParseable(contentType)) {
            LOGGER.debugf("Rejecting request with malformed Content-Type header: %s", contentType);
            routingContext.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .end(MALFORMED_CONTENT_TYPE_RESPONSE);
            return;
        }

        routingContext.next();
    }

    private static boolean isParseable(String contentType) {
        try {
            MediaType.valueOf(contentType);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
