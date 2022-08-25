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

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.health.DataSourceHealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Keycloak Healthcheck Readiness Probe.
 *
 * Performs a hybrid between the passive and the active mode. If there are no healthy connections in the pool,
 * it invokes the standard <code>DataSourceHealthCheck</code> that creates a new connection and checks if its valid.
 *
 * @see <a href="https://github.com/keycloak/keycloak-community/pull/55">Healthcheck API Design</a>
 */
@Readiness
@ApplicationScoped
public class KeycloakReadyHealthCheck extends DataSourceHealthCheck {

    /**
     * Date formatter, the same as used by Quarkus. This enables users to quickly compare the date printed
     * by the probe with the logs.
     */
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").withZone(ZoneId.systemDefault());

    @Inject
    AgroalDataSource agroalDataSource;

    AtomicReference<Instant> failingSince = new AtomicReference<>();

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Keycloak database connections health check").up();
        long activeCount = agroalDataSource.getMetrics().activeCount();
        long invalidCount = agroalDataSource.getMetrics().invalidCount();
        if (activeCount < 1 || invalidCount > 0) {
            HealthCheckResponse activeCheckResult = super.call();
            if (activeCheckResult.getStatus() == HealthCheckResponse.Status.DOWN) {
                builder.down();
                Instant failingTime = failingSince.updateAndGet(this::createInstanceIfNeeded);
                builder.withData("Failing since", DATE_FORMATTER.format(failingTime));
            }
        } else {
            failingSince.set(null);
        }
        return builder.build();
    }

    Instant createInstanceIfNeeded(Instant instant) {
        if (instant == null) {
            return Instant.now();
        }
        return instant;
    }
}