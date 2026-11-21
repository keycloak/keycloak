/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.services.metrics;

import jakarta.inject.Singleton;

import org.keycloak.common.Version;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Exposes the Keycloak version as an info-style constant gauge on the metrics endpoint, following the Prometheus
 * {@code *_info} convention also used by {@code jvm_info}. It is registered whenever metrics are enabled and allows
 * inventory and monitoring tools to determine the Keycloak version of an instance without needing an admin token for
 * {@code /admin/serverinfo}.
 * <p>
 * Micrometer applies all {@link MeterBinder} beans to the global registry when metrics are enabled.
 */
@Singleton
public class KeycloakBuildInfoMeter implements MeterBinder {

    // Rendered as `keycloak_build_info` by the Prometheus registry, which follows the Micrometer naming convention.
    public static final String METER_NAME = "keycloak.build.info";

    private static final String VERSION_TAG = "version";
    private static final String DESCRIPTION = "Keycloak server version";

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(METER_NAME, () -> 1)
                .tag(VERSION_TAG, Version.VERSION)
                .description(DESCRIPTION)
                .register(registry);
    }
}
