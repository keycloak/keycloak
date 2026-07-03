/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.services;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

/**
 * Rejects requests whose Host header does not match the configured hostname with HTTP 421 (Misdirected Request).
 * This prevents HTTP/2 connection coalescing issues when wildcard certificates are used; browsers should gracefully retry.
 * It also uncovers misconfigured reverse proxies that do not forward the original HTTP host; in those cases requests will fail.
 */
public class RejectMisdirectedRequestFilter implements Handler<RoutingContext> {
    private static final Logger LOGGER = Logger.getLogger(RejectMisdirectedRequestFilter.class);
    private Set<String> allowedHosts;
    private List<String> managementPaths;

    public void configure(Set<String> allowedHosts, List<String> managementPaths) {
        this.allowedHosts = allowedHosts;
        this.managementPaths = managementPaths;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            routingContext.next();
            return;
        }

        HostAndPort authority = routingContext.request().authority();
        if (authority == null) {
            routingContext.next();
            return;
        }

        String hostOnly = authority.host().toLowerCase(Locale.ROOT);
        if (allowedHosts.contains(hostOnly)) {
            routingContext.next();
            return;
        }

        if (managementPaths != null && isManagementPath(routingContext.normalizedPath())) {
            routingContext.next();
            return;
        }

        LOGGER.warnf("Misdirected request rejected: Host header '%s' does not match the configured hostname(s) %s. "
                + "This can happen due to a misconfigured reencrypt or edge terminating reverse proxy that does not forward the original hostname, "
                + "or due to HTTP/2 connection coalescing when a wildcard certificate is used. "
                + "For a reencrypt or edge terminating reverse proxy, configure it to forward the original hostname "
                + "and set 'proxy-headers' to 'forwarded' or 'xforwarded' for Keycloak depending on how the proxy forwards the information. "
                + "For a TLS passthrough proxy, use a dedicated certificate for Keycloak instead of a wildcard certificate. "
                + "To disable this check, set 'hostname-strict-host-check-enabled=false'.", authority.host(), allowedHosts);
        routingContext.response()
                .setStatusCode(HttpResponseStatus.MISDIRECTED_REQUEST.code())
                .setStatusMessage(HttpResponseStatus.MISDIRECTED_REQUEST.reasonPhrase())
                .putHeader("Content-Type", "text/plain; charset=UTF-8")
                .end(HttpResponseStatus.MISDIRECTED_REQUEST.reasonPhrase());
    }

    private boolean isManagementPath(String path) {
        for (String managementPath : managementPaths) {
            if (path.equals(managementPath) || path.startsWith(managementPath + "/")) {
                return true;
            }
        }
        return false;
    }

}
