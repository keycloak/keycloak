package org.keycloak.quarkus.runtime.services;

import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class MisdirectedFilter implements Handler<RoutingContext> {
    
    private final Set<String> allowedHosts;
    
    public MisdirectedFilter(Set<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if ("http".equals(routingContext.request().scheme())) {
            routingContext.next();
            return;
        }
        var request = routingContext.request();
        var authority = request.authority();
        String indicatedName = request.connection().indicatedServerName();
        if (authority == null || indicatedName == null || indicatedName.equalsIgnoreCase(authority.host())
                || allowedHosts.contains(authority.host())) {
            routingContext.next();
        } else {
            routingContext.response()
                .setStatusCode(HttpResponseStatus.MISDIRECTED_REQUEST.code())
                .setStatusMessage(HttpResponseStatus.MISDIRECTED_REQUEST.reasonPhrase())
                .putHeader("Content-Type", "text/plain; charset=UTF-8")
                .end(HttpResponseStatus.MISDIRECTED_REQUEST.reasonPhrase());
        }
    }

}
