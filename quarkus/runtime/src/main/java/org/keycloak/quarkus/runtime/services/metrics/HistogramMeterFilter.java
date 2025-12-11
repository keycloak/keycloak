/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Singleton;

import org.keycloak.config.HttpOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * @author Alexander Schwartz
 */
@Singleton
public class HistogramMeterFilter implements MeterFilter {

    private boolean histogramsEnabled;
    private double[] slos;

    public HistogramMeterFilter() {
        histogramsEnabled = Configuration.isTrue(HttpOptions.HTTP_METRICS_HISTOGRAMS_ENABLED);
        Optional<String> slosOption = Configuration.getOptionalKcValue(HttpOptions.HTTP_METRICS_SLOS.getKey());
        if (slosOption.isPresent()) {
            slos = Arrays.stream(slosOption.get().split(",")).filter(s -> !s.trim().isEmpty()).mapToDouble(s -> TimeUnit.MILLISECONDS.toNanos(Long.parseLong(s))).toArray();
            if (slos.length == 0) {
                slos = null;
            }
        }
    }

    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (isHttpServerRequests(id)) {
            DistributionStatisticConfig.Builder builder = DistributionStatisticConfig.builder()
                    .percentilesHistogram(histogramsEnabled);
            if (slos != null) {
                builder.serviceLevelObjectives(slos);
            }
            return builder.build().merge(config);
        }
        return config;
    }

    private boolean isHttpServerRequests(Meter.Id id) {
        return "http.server.requests".equals(id.getName());
    }
}
