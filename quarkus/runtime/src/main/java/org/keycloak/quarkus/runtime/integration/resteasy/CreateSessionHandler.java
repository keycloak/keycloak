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

import static org.keycloak.common.util.Resteasy.clearContextData;

import jakarta.ws.rs.container.CompletionCallback;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.vertx.ext.web.RoutingContext;

public final class CreateSessionHandler implements ServerRestHandler, TransactionalSessionHandler, CompletionCallback {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        QuarkusResteasyReactiveRequestContext context = (QuarkusResteasyReactiveRequestContext) requestContext;
        RoutingContext routingContext = context.getContext();
        KeycloakSession currentSession = routingContext.get(KeycloakSession.class.getName());

        if (currentSession == null) {
            // this handler might be invoked multiple times when resolving sub-resources
            // make sure the session is created once
            KeycloakSession session = create();
            routingContext.put(KeycloakSession.class.getName(), session);
            context.registerCompletionCallback(this);
            Resteasy.pushContext(KeycloakSession.class, session);
        }
    }

    @Override
    public void onComplete(Throwable throwable) {
        try {
            close(Resteasy.getContextData(KeycloakSession.class));
        } catch (Exception e) {

        }
        clearContextData();
    }
}
