/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.filter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.inject.spi.CDI;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.runtime.shutdown.ShutdownListener;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.http.impl.Http2ServerConnection;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

/**
 * Provide a graceful shutdown. Inspired by {@link io.quarkus.vertx.http.runtime.filters.GracefulShutdownFilter}.
 * <p>
 * This explores options while we wait for <a href="https://github.com/keycloak/keycloak/issues/43589">#43589</a>.
 */
public class KeycloakGracefulShutdownFilter implements ShutdownListener, Handler<RoutingContext> {

    private static final Logger log = Logger.getLogger(KeycloakGracefulShutdownFilter.class);

    private volatile boolean running = true;
    private final AtomicInteger currentRequestCount = new AtomicInteger();
    private final AtomicReference<ShutdownNotification> notification = new AtomicReference<>();

    private final Handler<AsyncResult<Void>> requestDoneHandler = event -> {
        int count = currentRequestCount.decrementAndGet();
        if (!running && count == 0) {
            executeNotification();
        }
    };
    private volatile Long timer;

    private void executeNotification() {
        ShutdownNotification n = notification.get();
        if (n != null && notification.compareAndSet(n, null)) {
            log.info("All HTTP requests complete");
            n.done();
            if (timer != null) {
                getVertx().cancelTimer(timer);
            }
        }
    }

    private Vertx getVertx() {
        return CDI.current().select(Vertx.class).get();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        currentRequestCount.incrementAndGet();
        routingContext.addEndHandler(requestDoneHandler);
        if (routingContext.request().version() == HttpVersion.HTTP_1_1) {
            // For HTTP/1.1, check in the last moment where we can add the header if the shutdown is in progress
            routingContext.addHeadersEndHandler(event -> {
                if (!running) {
                    final Http1xServerConnection connection = (Http1xServerConnection) routingContext.request().connection();
                    // "Connection-specific header fields such as Connection and Keep-Alive are prohibited in HTTP/2 and HTTP/3"
                    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Connection
                    // Therefore only add it for HTTP/1.1
                    routingContext.response().headers().add(HttpHeaderNames.CONNECTION, "close");
                    routingContext.addEndHandler(e -> {
                        connection.close();
                    });
                }
            });
        }
        else if (routingContext.request().version() == HttpVersion.HTTP_2) {
            if (!running) {
                // If shutdown is in progress, send the go away as early as possible
                sendGoAwayForHttp2(routingContext);
            } else {
                routingContext.addEndHandler(event -> {
                    // Check again at the end of the request if we should send a shutdown
                    if (!running) {
                        sendGoAwayForHttp2(routingContext);
                    }
                });
            }
        }
        routingContext.next();
    }

    private static void sendGoAwayForHttp2(RoutingContext routingContext) {
        final Http2ServerConnection connection = (Http2ServerConnection) routingContext.request().connection();
        // GO_AWAY + 0 (NO_ERROR) = graceful shutdown; client will stop creating new streams on this connection
        connection.goAway(0);
    }

    @Override
    public void preShutdown(ShutdownNotification notification) {
        this.notification.set(notification);
        running = false;
        if (currentRequestCount.get() == 0) {
            if (this.notification.compareAndSet(notification, null)) {
                notification.done();
            }
        } else {
            log.info("Waiting for HTTP requests to complete");
            timer = getVertx().setTimer(TimeUnit.SECONDS.toMillis(10), event -> {
                if (this.notification.compareAndSet(notification, null)) {
                    log.info("Timeout reached waiting for requests to complete");
                    notification.done();
                }
            });
        }
    }

}
