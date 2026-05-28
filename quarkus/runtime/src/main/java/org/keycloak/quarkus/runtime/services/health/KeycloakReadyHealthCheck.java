/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.quarkus.runtime.services.health;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.agroal.runtime.health.DataSourceHealthCheck;
import io.quarkus.smallrye.health.runtime.QuarkusAsyncHealthCheckFactory;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

/**
 * Keycloak Healthcheck Readiness Probe.
 * <p>
 * Converts the standard <code>DataSourceHealthCheck</code> that waits for a connection to be returned to the pool and checks if it's valid, to an async check.
 * <p>
 *
 * @see <a href="https://github.com/keycloak/keycloak-community/pull/55">Healthcheck API Design</a>
 */
@Readiness
@ApplicationScoped
public class KeycloakReadyHealthCheck implements AsyncHealthCheck {

    public static final String FAILING_SINCE = "Failing since";

    /**
     * Date formatter, the same as used by Quarkus. This enables users to quickly compare the date printed
     * by the probe with the logs.
     */
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(ZoneId.systemDefault());

    @Inject
    QuarkusAsyncHealthCheckFactory healthCheckFactory;

    @Inject
    DataSourceHealthCheck dataSourceHealthCheck;

    @Inject
    @ManagedExecutorConfig(maxAsync = 1, maxQueued = 20)
    ManagedExecutor executor;

    private final AtomicReference<Instant> failingSince = new AtomicReference<>();

    @Override
    public Uni<HealthCheckResponse> call() {
        Uni<HealthCheckResponse> uni = Uni.createFrom().item(this::syncCheck);
        return uni.runSubscriptionOn(executor);
    }

    private HealthCheckResponse syncCheck() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Keycloak database connections async health check").up();
        HealthCheckResponse activeCheckResult = dataSourceHealthCheck.call();
        if (activeCheckResult.getStatus() == HealthCheckResponse.Status.DOWN) {
            builder.down();
            Instant failingTime = failingSince.updateAndGet(KeycloakReadyHealthCheck::createInstanceIfNeeded);
            builder.withData(FAILING_SINCE, DATE_FORMATTER.format(failingTime));
        } else {
            failingSince.set(null);
        }
        return builder.build();
    }

    static Instant createInstanceIfNeeded(Instant instant) {
        return Objects.requireNonNullElseGet(instant, Instant::now);
    }
}
