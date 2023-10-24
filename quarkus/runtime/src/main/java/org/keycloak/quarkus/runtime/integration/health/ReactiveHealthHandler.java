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

package org.keycloak.quarkus.runtime.integration.health;

import io.quarkus.smallrye.health.runtime.SmallRyeLivenessHandler;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * This adds the possibility to have a non-blocking health handler in Quarkus.
 * <p>
 * Without a non-blocking health check, all liveness and readiness probes will enqueue in the worker thread pool. Under high load
 * of if there is a lot of blocking IO happening (for example, during Keycloak cluster rebalancing), this leads to probes being queued.
 * Queued probes would lead to timeouts unless the timeouts are configured to 10-20 seconds. Reactive probes avoid the enqueueing
 * in the worker thread pool for all non-blocking probes, which will be the default for the (otherwise empty) liveness probe.
 * For the readiness probe, this depends on the implementation of the specific readiness probes.
 * <p>
 * This is a workaround until <a href="https://github.com/quarkusio/quarkus/pull/35100">quarkusio/quarkus#35100</a> is available
 * in a regular Quarkus version. Then these classes can be removed.
 *
 * @author Alexander Schwartz
 */
public abstract class ReactiveHealthHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext context) {
        Uni<SmallRyeHealth> health = getHealth();
        health.subscribe().with(smallRyeHealth -> {
            new SmallRyeLivenessHandler() {
                @Override
                protected SmallRyeHealth getHealth(SmallRyeHealthReporter reporter, RoutingContext ctx) {
                    return smallRyeHealth;
                }
            }.handle(context);
        });
    }

    protected abstract Uni<SmallRyeHealth> getHealth();
}
