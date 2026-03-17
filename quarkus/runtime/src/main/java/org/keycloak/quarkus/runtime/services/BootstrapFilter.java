package org.keycloak.quarkus.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

import org.keycloak.services.resources.KeycloakApplication;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Pre-matching request filter that returns a 503 Service Unavailable response while the server bootstrap is in progress.
 */
@ApplicationScoped
public class BootstrapFilter {

    private boolean ready;

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
        // Return 503 Service Unavailable
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();

    }
}
