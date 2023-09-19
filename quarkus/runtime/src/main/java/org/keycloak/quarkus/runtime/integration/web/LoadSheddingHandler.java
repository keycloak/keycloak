/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.keycloak.quarkus.runtime.configuration.Configuration;

/**
 * Limit the number of concurrent requests (both blocking and non-blocking) to a configured upper limit.
 * Special paths (like for health probes and metrics) can be excluded, so their requests will not be dropped.
 *
 * @author Alexander Schwartz
 */
public class LoadSheddingHandler implements Handler<RoutingContext> {

    private final Logger logger = Logger.getLogger(LoadSheddingHandler.class);

    private final Predicate<RoutingContext> contextFilter;

    private final Semaphore available;

    public LoadSheddingHandler() {
        this(null);
    }

    public LoadSheddingHandler(Predicate<RoutingContext> contextFilter) {
        Optional<String> maxConcurrentRequests = Configuration.getRuntimeProperty("kc.http-max-concurrent-requests");
        if (maxConcurrentRequests.isPresent()) {
            available = new Semaphore(Integer.parseInt(maxConcurrentRequests.get()), false);
        } else {
            available = null;
        }
        this.contextFilter = contextFilter;
    }

    private final LongAdder rejectedRequests = new LongAdder();
    private volatile boolean loadSheddingActive;

    @Override
    public void handle(RoutingContext context) {
        if (available == null || ignoreContext(context)) {
            context.next();
            return;
        }
        if (available.tryAcquire()) {
            context.addEndHandler(event -> {
                available.release();
            });
            if (loadSheddingActive) {
                Long req = null;
                synchronized (rejectedRequests) {
                    if (loadSheddingActive) {
                        loadSheddingActive = false;
                        // rejectedRequests.sumThenReset() is approximative when concurrent increments are active, still it should be accurate enough for this log message
                        req =  rejectedRequests.sumThenReset();
                    }
                }
                if (req != null) {
                    // this avoids logging in a synchronized block
                    logger.warnf("Executor thread pool no longer exhausted, request processing continued after %s discarded request(s)", req);
                }
            }
            context.next();
        } else {
            boolean logExhausted = false;
            if (!loadSheddingActive) {
                synchronized (rejectedRequests) {
                    if (!loadSheddingActive) {
                        loadSheddingActive = true;
                        logExhausted = true;
                    }
                }
            }
            if (logExhausted) {
                // this avoids logging in a synchronized block
                logger.warn("Executor thread pool exhausted, starting to reject requests");
            }
            rejectedRequests.increment();
            // if the thread pool has been configured with a maximum queue size, it might reject the request
            context.fail(HttpStatus.SC_SERVICE_UNAVAILABLE);
        }
    }

    private boolean ignoreContext(RoutingContext context) {
        return contextFilter != null && contextFilter.test(context);
    }

}
