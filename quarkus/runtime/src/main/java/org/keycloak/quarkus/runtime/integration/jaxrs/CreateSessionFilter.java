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

package org.keycloak.quarkus.runtime.integration.jaxrs;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.vertx.ext.web.RoutingContext;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class CreateSessionFilter implements ResteasyReactiveContainerRequestFilter {

    @Override
    public void filter(ResteasyReactiveContainerRequestContext requestContext) {
        QuarkusResteasyReactiveRequestContext context = (QuarkusResteasyReactiveRequestContext) requestContext.getServerRequestContext();
        RoutingContext routingContext = context.getContext();
        KeycloakSession currentSession = routingContext.get(KeycloakSession.class.getName());

        if (currentSession == null) {
            // this handler might be invoked multiple times when resolving sub-resources
            // make sure the session is created once
            KeycloakSessionFactory sessionFactory = QuarkusKeycloakSessionFactory.getInstance();
            KeycloakSession session = sessionFactory.create();
            routingContext.put(KeycloakSession.class.getName(), session);
            // the CloseSessionFilter is needed because it runs sooner than this callback
            // this is just a catch-all if the CloseSessionFilter doesn't get a chance to run
            context.registerCompletionCallback(ignored -> {
                try {
                    session.close();
                } catch (Exception e) {
                    // we don't care about the exception, the session may already be closed
                }
            });
        }
    }

}
