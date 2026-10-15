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

import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;

@Singleton
public class JGroupsMeterFilter implements MeterFilter {

    private final boolean renameClusterLabel = Configuration.getOptionalBooleanKcValue(
            "spi-cache-embedded--default--metrics-rename-cluster-label").orElse(false);
    // Prometheus converts the dot in Infinispan's raw meter names to an underscore during export.
    private final MeterFilter rename = MeterFilter.renameTag("vendor.jgroups_", "cluster", "jgroups_cluster");

    @Override
    public Meter.Id map(Meter.Id id) {
        return renameClusterLabel ? rename.map(id) : id;
    }
}
