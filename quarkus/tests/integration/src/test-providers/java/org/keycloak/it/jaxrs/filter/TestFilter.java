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

package org.keycloak.it.jaxrs.filter;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class TestFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(TestFilter.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    @Context
    KeycloakSession session;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final String method = requestContext.getMethod();
        final String path = info.getPath();
        final SocketAddress address = request.remoteAddress();

        KeycloakSession s = null;
        try {
            if (session != null) {
                session.getContext();
            }
            s = session;
        } catch (RuntimeException e) {
            // should say something like Normal scoped producer method may not return null: org.keycloak.quarkus.runtime.integration.cdi.KeycloakBeanProducer.getKeycloakSession()
        }

        LOG.infof("Request %s %s has context request %s has keycloaksession %s", method, path, address != null, s != null);
    }
}
