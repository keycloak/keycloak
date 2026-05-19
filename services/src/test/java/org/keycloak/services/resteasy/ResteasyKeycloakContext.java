/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.resteasy;

import java.util.Optional;

import org.keycloak.common.ClientConnection;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.DefaultKeycloakContext;

import org.jboss.resteasy.core.ResteasyContext;

public class ResteasyKeycloakContext extends DefaultKeycloakContext {

    public ResteasyKeycloakContext(KeycloakSession session) {
        super(session);
    }

    @Override
    protected Optional<HttpRequest> createHttpRequest() {
        return Optional.ofNullable(ResteasyContext.getContextData(org.jboss.resteasy.spi.HttpRequest.class)).map(HttpRequestImpl::new);
    }

    @Override
    protected Optional<HttpResponse> createHttpResponse() {
        return Optional.ofNullable(ResteasyContext.getContextData(org.jboss.resteasy.spi.HttpResponse.class)).map(HttpResponseImpl::new);
    }

    @Override
    public ClientConnection getConnection() {
        throw new UnsupportedOperationException();
    }

}
