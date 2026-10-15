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
package org.keycloak.connections.httpclient;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.protocol.HttpContext;

/**
 * Route planner and DNS resolver in one, for a client that applies an address policy to the targets
 * of direct connections while still reaching configured proxies, which are commonly private.
 * <p>
 * Planning a route records, for the current thread, the proxy of that route, or that it has none.
 * Resolving a host then uses {@code proxyResolver}, with all of the proxy's addresses so the client
 * can fail over between them, only for the proxy of the current route; every other host, including
 * a proxy's host reached by a direct route, goes through {@code targetResolver}. This relies on the
 * client planning a request's route and then connecting for it on the same thread, without resolving
 * hosts for other requests in between, as Apache HttpClient's classic client does. Redirects must not
 * be followed, since a redirect connects without a new route being planned by this instance first.
 */
class ProxyRouteAwareDnsResolver implements HttpRoutePlanner, DnsResolver {

    private final HttpRoutePlanner delegate;
    private final DnsResolver targetResolver;
    private final DnsResolver proxyResolver;
    private final ThreadLocal<String> currentProxyHostName = new ThreadLocal<>();

    ProxyRouteAwareDnsResolver(HttpRoutePlanner delegate, DnsResolver targetResolver) {
        this(delegate, targetResolver, SystemDefaultDnsResolver.INSTANCE);
    }

    ProxyRouteAwareDnsResolver(HttpRoutePlanner delegate, DnsResolver targetResolver, DnsResolver proxyResolver) {
        this.delegate = delegate;
        this.targetResolver = targetResolver;
        this.proxyResolver = proxyResolver;
    }

    @Override
    public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        HttpRoute route = delegate.determineRoute(target, request, context);
        HttpHost proxy = route.getProxyHost();
        currentProxyHostName.set(proxy == null ? null : proxy.getHostName());
        return route;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        String proxyHostName = currentProxyHostName.get();
        if (proxyHostName != null && proxyHostName.equalsIgnoreCase(host)) {
            return proxyResolver.resolve(host);
        }
        return targetResolver.resolve(host);
    }
}
