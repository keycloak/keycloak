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

import org.keycloak.common.ClientConnection;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.DefaultKeycloakContext;

import io.vertx.core.http.HttpServerRequest;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public final class QuarkusKeycloakContext extends DefaultKeycloakContext {

    public QuarkusKeycloakContext(KeycloakSession session) {
        super(session);
    }

    @Override
    protected HttpRequest createHttpRequest() {
        return new QuarkusHttpRequest(getResteasyReactiveRequestContext());
    }

    @Override
    protected HttpResponse createHttpResponse() {
        return new QuarkusHttpResponse(getResteasyReactiveRequestContext());
    }

    @Override
    protected ClientConnection createClientConnection() {
        ResteasyReactiveRequestContext requestContext = getResteasyReactiveRequestContext();
        HttpServerRequest serverRequest = requestContext.unwrap(HttpServerRequest.class);
        return new QuarkusClientConnection(serverRequest);
    }

    private ResteasyReactiveRequestContext getResteasyReactiveRequestContext() {
        return CurrentRequestManager.get();
    }
}
