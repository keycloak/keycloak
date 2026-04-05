package org.keycloak.quarkus.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.services.resources.KeycloakApplication;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Pre-matching request filter that returns a 503 Service Unavailable response while the server bootstrap is in progress.
 */
@ApplicationScoped
public class BootstrapFilter {
    private static final Logger LOG = Logger.getLogger(BootstrapFilter.class);

    private final long startup;
    private boolean ready;
    private volatile boolean warningLogged;

    public BootstrapFilter() {
        startup = System.currentTimeMillis();
    }

    @ServerRequestFilter(priority = 1, preMatching = true)
    public Response filter(ContainerRequestContext ignored) {
        if (ready) {
            // JVM branch prediction may optimize this code and saves on reading a static volatile field
            return null;
        }
        if (KeycloakApplication.isBootstrapCompleted()) {
            // Return null to continue the request chain normally
            ready = true;
            return null;
        }
        if (!warningLogged) {
            synchronized (this) {
                if (!warningLogged) {
                    // Log the warning at most once, so admins knows that they should do something before some client complains
                    LOG.warn("Request received during bootstrapping, returning a 503 error. Use the readiness health endpoint to ensure the service is ready before forwarding requests to the service.");
                }
                warningLogged = true;
            }
        }

        // Implement a back-off to wait as long as the current start-up took, but then retry at least once per minute
        long retry = Math.min(Math.max((System.currentTimeMillis() - startup) / 1000, 1), 60);
        // Return 503 Service Unavailable
        return Response
                .status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.TEXT_PLAIN)
                .entity("Boostrap in progress. Retry in " + retry + " seconds.")
                .header(HttpHeaders.RETRY_AFTER, retry)
                .header("Refresh", retry)
                .build();

    }
}
