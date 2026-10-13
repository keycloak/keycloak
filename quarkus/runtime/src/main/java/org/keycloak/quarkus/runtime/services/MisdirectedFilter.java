package org.keycloak.quarkus.runtime.services;

import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Defends against HTTP/2 connection coalescing delivering requests to the wrong backend.
 *
 * <p>When multiple hostnames share a TLS certificate (e.g. a wildcard cert behind a reverse proxy),
 * browsers may reuse a single HTTP/2 connection for requests to different hostnames (RFC 9113 §9.1.1).
 * In TLS passthrough mode the proxy routes by SNI, so all requests on a coalesced connection land on
 * the backend selected during the original TLS handshake — even if the {@code :authority} names a
 * different service. This filter returns HTTP 421 Misdirected Request for such requests, prompting
 * the client to open a new connection with the correct SNI.
 *
 * <p>Only active in TLS passthrough mode (no proxy headers configured). Registered by
 * {@link org.keycloak.quarkus.runtime.KeycloakRecorder#misdirectedRequestFilter}.
 *
 * <p>Cases that pass through:
 * <ul>
 *   <li>Plain HTTP requests — no TLS, no SNI to check.</li>
 *   <li>No {@code :authority} header — nothing to compare against; generally this is not 
 *       a valid http/2 request in any case.</li>
 *   <li>No SNI sent ({@code indicatedServerName()} is null) — happens with health checks
 *       or non-FQDN connections; rejecting these would break monitoring.</li>
 *   <li>SNI matches authority — the connection was established for this hostname.
 *       This allows all backend requests, regardless of the hostname, to succeed.</li>
 *   <li>Authority matches a configured Keycloak hostname ({@code hostname} or
 *       {@code hostname-admin}) — the request is legitimately for this server, even if
 *       the connection was originally established for a different hostname. NOTE: usage
 *       of realm specific front-end URLs may cause erroneous 421s as we're not attempting
 *       to maintain a cache of these values as they are directly modifiable while the server
 *       is running - this will be revisited if needed.</li>
 * </ul>
 *
 * <p>Note: {@code indicatedServerName()} returns the SNI from the initial TLS handshake and is
 * a per-connection value, not per-request. All HTTP/2 streams multiplexed on the same connection
 * share it.
 */
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
