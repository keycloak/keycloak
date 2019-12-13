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
import org.jboss.resteasy.core.interception.jaxrs.ContainerResponseContextImpl;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.services.resources.KeycloakApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;


@PreMatching
@Provider
@Priority(1)
public class QuarkusFilter implements javax.ws.rs.container.ContainerRequestFilter,
        javax.ws.rs.container.ContainerResponseFilter  {

    private static final Logger LOGGER = LoggerFactory.getLogger("QuarkusFilter");

    @Inject
    KeycloakApplication keycloakApplication;

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {

        if (containerRequestContext.getMediaType() == null) {
            containerRequestContext.getHeaders().add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        }

        Resteasy.pushContext(KeycloakApplication.class, keycloakApplication);

        KeycloakSessionFactory sessionFactory = keycloakApplication.getSessionFactory();

        KeycloakSession session = sessionFactory.create();

        Resteasy.pushContext(KeycloakSession.class, session);
        ClientConnection connection = new ClientConnection() {
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
        session.getContext().setConnection(connection);
        Resteasy.pushContext(ClientConnection.class, connection);

        KeycloakTransaction tx = session.getTransactionManager();

        Resteasy.pushContext(KeycloakTransaction.class, tx);
        tx.begin();
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {

        //Work around for https://github.com/vert-x3/vertx-web/issues/1340, or at least something that exhibits the same symptoms
        //Multiple Set-Cookie headers fail to be returned to the FE, with only 1 making it into the generated response.
        //However, multiple "NewCookie"'s work but then are issues around quoting the cookie value
        //See org.keycloak.common.util.ServerCookie on line 183.
        if (((ContainerResponseContextImpl) containerResponseContext).getHttpResponse().getOutputHeaders().get("Set-Cookie") != null
                &&  ((ContainerResponseContextImpl) containerResponseContext).getHttpResponse().getOutputHeaders().get("Set-Cookie").size() > 1) {
            ArrayList<String> list = (ArrayList) ((ContainerResponseContextImpl) containerResponseContext).getHttpResponse().getOutputHeaders().get("Set-Cookie");
            ArrayList<NewCookie> newCookies = new ArrayList<>();
            String keycloakSession = "";
            for (String item: list) {
                //NewCookie adds quotes around the cookie value for the KEYCLOAK_SESSION (due to /), so skipping this one.
                if (item.contains("KEYCLOAK_SESSION")) {
                    keycloakSession = item;
                } else {
                    newCookies.add(cookieParser(item));
                }
            };

            list.clear();

            if (!keycloakSession.isEmpty()) {
                list.add(keycloakSession);
            }

            newCookies.forEach(item -> {
                ((ContainerResponseContextImpl) containerResponseContext).getHttpResponse().addNewCookie(item);
            });
        }

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

    private NewCookie cookieParser(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return NewCookie.valueOf(value);
    }
}