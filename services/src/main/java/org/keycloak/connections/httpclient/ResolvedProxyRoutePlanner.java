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
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.protocol.HttpContext;

/**
 * Resolves the proxy of a proxied route with the system resolver and pins its address in the route,
 * so the client's own DNS resolver is only ever used for the targets of direct connections. A client
 * can then apply an address policy to every direct target without exempting the proxy by name.
 */
class ResolvedProxyRoutePlanner implements HttpRoutePlanner {

    private final HttpRoutePlanner delegate;

    ResolvedProxyRoutePlanner(HttpRoutePlanner delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        HttpRoute route = delegate.determineRoute(target, request, context);
        HttpHost proxy = route.getProxyHost();
        if (proxy == null || proxy.getAddress() != null) {
            return route;
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(proxy.getHostName());
        } catch (UnknownHostException e) {
            // Left unresolved, the connection fails as it would have without this planner.
            return route;
        }
        HttpHost resolvedProxy = new HttpHost(address, proxy.getHostName(), proxy.getPort(), proxy.getSchemeName());
        return new HttpRoute(route.getTargetHost(), route.getLocalAddress(), resolvedProxy, route.isSecure());
    }
}
