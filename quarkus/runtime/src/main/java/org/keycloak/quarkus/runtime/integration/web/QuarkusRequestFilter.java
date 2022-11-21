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

import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.transaction.TransactionalSessionHandler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * <p>This filter is responsible for managing the request lifecycle as well as setting up the necessary context to process incoming
 * requests. We need this filter running on the top of the chain in order to push contextual objects before executing Resteasy. It is not
 * possible to use a {@link javax.ws.rs.container.ContainerRequestFilter} for this purpose because some mechanisms like error handling
 * will not be able to access these contextual objects.
 * 
 * <p>The filter itself runs in an event loop and should delegate to worker threads any blocking code (for now, all requests are handled
 * as blocking).
 *
 * <p>Note that this filter is only responsible to close the {@link KeycloakSession} if not already closed when running Resteasy code. The reason is that closing it should be done at the
 * Resteasy level so that we don't block event loop threads even if they execute in a worker thread. Vert.x handlers and their
 * callbacks are not designed to run blocking code. If the session is eventually closed here is because Resteasy was not executed.
 *
 * @see org.keycloak.quarkus.runtime.integration.jaxrs.TransactionalResponseInterceptor
 * @see org.keycloak.quarkus.runtime.integration.jaxrs.TransactionalResponseFilter
 */
public class QuarkusRequestFilter implements Handler<RoutingContext>, TransactionalSessionHandler {

    private final ExecutorService executor;

    private Predicate<RoutingContext> contextFilter;

    public QuarkusRequestFilter() {
        this(null, null);
    }

    public QuarkusRequestFilter(Predicate<RoutingContext> contextFilter, ExecutorService executor) {
        this.contextFilter = contextFilter;
        this.executor = executor;
    }

    @Override
    public void handle(RoutingContext context) {
        if (ignoreContext(context)) {
            context.next();
            return;
        }
        // our code should always be run as blocking until we don't provide a better support for running non-blocking code
        // in the event loop
        executor.execute(createBlockingHandler(context));
    }

    private boolean ignoreContext(RoutingContext context) {
        return contextFilter != null && contextFilter.test(context);
    }

    private Runnable createBlockingHandler(RoutingContext context) {
        return () -> {
            KeycloakSession session = configureContextualData(context);

            try {
                context.next();
            } catch (Throwable cause) {
                // re-throw so that the any exception is handled from parent
                throw new RuntimeException(cause);
            } finally {
                // force closing the session if not already closed
                // under some circumstances resteasy might not be invoked like when no route is found for a particular path
                // in this case context is set with status code 404, and we need to close the session
                close(session);
            }
        };
    }

    private KeycloakSession configureContextualData(RoutingContext context) {
        KeycloakSession session = create();

        Resteasy.pushContext(KeycloakSession.class, session);
        context.put(KeycloakSession.class.getName(), session);

        ClientConnection connection = createClientConnection(context.request());

        Resteasy.pushContext(ClientConnection.class, connection);
        context.put(ClientConnection.class.getName(), connection);

        return session;
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
