package org.keycloak.services.filters;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
@PreMatching
@Priority(10)
public class InvalidContentTypeHeaderFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(InvalidContentTypeHeaderFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String contentType = requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            return;
        }

        if (contentType.isBlank()) {
            LOGGER.debugf("Request with invalid Content-Type header value is blocked");
            throw new BadRequestException("Invalid Content-Type header");
        }

        try {
            requestContext.getMediaType();
        } catch (IllegalArgumentException e) {
            LOGGER.debugf("Request with invalid Content-Type header value is blocked");
            throw new BadRequestException("Invalid Content-Type header");
        }
    }
}
