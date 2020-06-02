/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.provider.quarkus;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.services.resources.KeycloakApplication;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import java.io.IOException;


@PreMatching
@Provider
@Priority(1)
public class QuarkusFilter implements javax.ws.rs.container.ContainerRequestFilter,
        javax.ws.rs.container.ContainerResponseFilter  {

    @Inject
    KeycloakApplication keycloakApplication;
    
    @Inject
    RoutingContext routingContext;

    public QuarkusFilter() {
        //TODO: a temporary hack for https://github.com/quarkusio/quarkus/issues/9647, we need to disable the sanitizer to avoid
        // escaping text/html responses from the server
        Resteasy.getContextData(ResteasyDeployment.class).setProperty(ResteasyContextParameters.RESTEASY_DISABLE_HTML_SANITIZER, Boolean.TRUE);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        KeycloakSessionFactory sessionFactory = keycloakApplication.getSessionFactory();
        KeycloakSession session = sessionFactory.create();

        Resteasy.pushContext(KeycloakSession.class, session);
        HttpServerRequest request = routingContext.request();

        session.getContext().setConnection(createConnection(request));
        Resteasy.pushContext(ClientConnection.class, session.getContext().getConnection());

        KeycloakTransaction tx = session.getTransactionManager();
        Resteasy.pushContext(KeycloakTransaction.class, tx);

        tx.begin();
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        //End the session and clear context
        KeycloakSession session = Resteasy.getContextData(KeycloakSession.class);

        // KeycloakTransactionCommitter is responsible for committing the transaction, but if an exception is thrown it's not invoked and transaction
        // should be rolled back
        if (session.getTransactionManager() != null && session.getTransactionManager().isActive()) {
            session.getTransactionManager().rollback();
        }

        session.close();
        Resteasy.clearContextData();
    }

    private ClientConnection createConnection(HttpServerRequest request) {
        return new ClientConnection() {
            @Override
            public String getRemoteAddr() {
                return request.remoteAddress().host();
            }

            @Override
            public String getRemoteHost() {
                return request.remoteAddress().host();
            }

            @Override
            public int getRemotePort() {
                return request.remoteAddress().port();
            }

            @Override
            public String getLocalAddr() {
                return request.localAddress().host();
            }

            @Override
            public int getLocalPort() {
                return request.localAddress().port();
            }
        };
    }
}