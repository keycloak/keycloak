package org.keycloak.quarkus.runtime.services;

import org.keycloak.models.KeycloakSession;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

public class MisdirectedFilter implements Handler<RoutingContext> {
    
    @Inject
    KeycloakSession session;
    private final boolean hostnameStrict;
    private final String adminHost;
    
    public MisdirectedFilter(boolean hostnameStrict, String adminHost) {
        this.hostnameStrict = hostnameStrict;
        this.adminHost = adminHost;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if ("http".equals(routingContext.request().scheme())) {
            return;
        }
        var request = routingContext.request();
        var authority = request.authority();
        String indicatedName = request.connection().indicatedServerName();
        if (authority == null || indicatedName == null || indicatedName.equalsIgnoreCase(authority.host())) {
            return;
        }
        if (!hostnameStrict || !(authority.host().equalsIgnoreCase(adminHost)
                || authority.host().equalsIgnoreCase(session.getContext().getUri().getBaseUri().getHost()))) {
            throw new WebApplicationException(421);
        }
    }

}
