/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.web;

import static org.keycloak.services.resources.KeycloakApplication.getSessionFactory;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * <p>This filter is responsible for managing the request lifecycle as well as setting up the necessary context to process incoming
 * requests.
 * 
 * <p>The filter itself runs in a event loop and should delegate to worker threads any blocking code (for now, all requests are handled
 * as blocking).
 */
public class QuarkusRequestFilter implements Handler<RoutingContext> {

    private static final Handler<AsyncResult<Object>> EMPTY_RESULT = result -> {
        // we don't really care about the result because any exception thrown should be handled by the parent class
    };

    @Override
    public void handle(RoutingContext context) {
        // our code should always be run as blocking until we don't provide a better support for running non-blocking code
        // in the event loop
        context.vertx().executeBlocking(createBlockingHandler(context), false, EMPTY_RESULT);
    }

    private Handler<Promise<Object>> createBlockingHandler(RoutingContext context) {
        return promise -> {
            KeycloakSessionFactory sessionFactory = getSessionFactory();
            KeycloakSession session = sessionFactory.create();

            configureContextualData(context, createClientConnection(context.request()), session);
            configureEndHandler(context, promise, session);

            KeycloakTransactionManager tx = session.getTransactionManager();

            try {
                tx.begin();
                context.next();
                promise.complete();
            } catch (Throwable cause) {
                promise.fail(cause);
                // re-throw so that the any exception is handled from parent
                throw new RuntimeException(cause);
            } finally {
                if (!context.response().headWritten()) {
                    // make sure the session is closed in case the handler is not called
                    // it might happen that, for whatever reason, downstream handlers do not end the response or
                    // no data was written to the response
                    close(session);
                }
            }
        };
    }

    /**
     * Creates a handler to close the {@link KeycloakSession} before the response is written to response but after Resteasy
     * is done with processing its output.
     */
    private void configureEndHandler(RoutingContext context, Promise<Object> promise, KeycloakSession session) {
        context.addHeadersEndHandler(event -> {
            try {
                close(session);
            } catch (Throwable cause) {
                promise.fail(cause);
            }
        });
    }

    private void configureContextualData(RoutingContext context, ClientConnection connection, KeycloakSession session) {
        Resteasy.pushContext(ClientConnection.class, connection);
        Resteasy.pushContext(KeycloakSession.class, session);
        // quarkus-resteasy changed and clears the context map before dispatching
        // need to push keycloak contextual objects into the routing context for retrieving it later
        context.put(KeycloakSession.class.getName(), session);
        context.put(ClientConnection.class.getName(), connection);
    }

    protected void close(KeycloakSession session) {
        KeycloakTransactionManager tx = session.getTransactionManager();
        if (tx.isActive()) {
            if (tx.getRollbackOnly()) {
                tx.rollback();
            } else {
                tx.commit();
            }
        }
        session.close();
    }

    private ClientConnection createClientConnection(HttpServerRequest request) {
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
