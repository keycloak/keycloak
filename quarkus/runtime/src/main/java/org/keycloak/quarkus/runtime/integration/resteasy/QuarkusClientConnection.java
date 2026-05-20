/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.resteasy;

import java.util.Optional;

import org.keycloak.common.ClientConnection;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

public final class QuarkusClientConnection implements ClientConnection {

    private final HttpServerRequest request;

    public QuarkusClientConnection(HttpServerRequest request) {
        this.request = request;
    }

    @Override
    public String getRemoteAddr() {
        return Optional.ofNullable(request.remoteAddress()).map(SocketAddress::hostAddress).orElse(null);
    }

    @Override
    public String getRemoteHost() {
        return Optional.ofNullable(request.remoteAddress()).map(SocketAddress::host).orElse(null);
    }

    @Override
    public int getRemotePort() {
        return Optional.ofNullable(request.remoteAddress()).map(SocketAddress::port).orElse(0);
    }

    @Override
    public String getLocalAddr() {
        return Optional.ofNullable(request.localAddress()).map(SocketAddress::hostAddress).orElse(null);
    }

    @Override
    public int getLocalPort() {
        return Optional.ofNullable(request.localAddress()).map(SocketAddress::port).orElse(0);
    }
}
