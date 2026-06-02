package org.keycloak.services.filters;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
@PreMatching
@Priority(10)
public class InvalidProxyRequestFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(InvalidProxyRequestFilter.class);

    private static final String FORWARDED = "Forwarded";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String INVALID_REQUEST_SCHEME = "Invalid request scheme";
    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!hasProxySchemeHeader(requestContext) || isValidRequestScheme(requestContext.getUriInfo().getBaseUri().getScheme())) {
            return;
        }

        LOGGER.debugf("Request with invalid proxy-derived scheme is blocked");
        requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST)
                .entity(INVALID_REQUEST_SCHEME)
                .build());
    }

    private boolean hasProxySchemeHeader(ContainerRequestContext requestContext) {
        return requestContext.getHeaderString(X_FORWARDED_PROTO) != null || requestContext.getHeaderString(FORWARDED) != null;
    }

    private boolean isValidRequestScheme(String scheme) {
        return HTTP.equalsIgnoreCase(scheme) || HTTPS.equalsIgnoreCase(scheme);
    }
}
