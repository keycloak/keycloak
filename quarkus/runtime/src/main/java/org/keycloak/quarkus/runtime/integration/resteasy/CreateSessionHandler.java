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

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.vertx.ext.web.RoutingContext;

public final class CreateSessionHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        QuarkusResteasyReactiveRequestContext context = (QuarkusResteasyReactiveRequestContext) requestContext;
        RoutingContext routingContext = context.getContext();
        KeycloakSession currentSession = routingContext.get(KeycloakSession.class.getName());

        if (currentSession == null) {
            // this handler might be invoked multiple times when resolving sub-resources
            // make sure the session is created once
            KeycloakSessionFactory sessionFactory = QuarkusKeycloakSessionFactory.getInstance();
            KeycloakSession session = sessionFactory.create();
            session.getTransactionManager().begin();
            routingContext.put(KeycloakSession.class.getName(), session);
            // the CloseSessionFilter is needed because it runs sooner than this callback
            // this is just a catch-all if the CloseSessionFilter doesn't get a chance to run
            context.registerCompletionCallback(ignored -> {
                try {
                    session.close();
                } catch (Exception e) {

                }
            });
        }
    }

}
