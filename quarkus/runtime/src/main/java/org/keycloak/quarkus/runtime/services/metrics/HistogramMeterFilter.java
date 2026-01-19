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

    private final boolean httpClientHistograms;
    private final boolean httpServerHistograms;
    private final double[] httpClientSLOs;
    private final double[] httpServerSLOs;

    public HistogramMeterFilter() {
        this.httpClientHistograms = Configuration.isTrue(HttpOptions.HTTP_CLIENT_METRICS_HISTOGRAMS_ENABLED);
        this.httpClientSLOs = slo(Configuration.getOptionalKcValue(HttpOptions.HTTP_CLIENT_METRICS_SLOS));
        this.httpServerHistograms = Configuration.isTrue(HttpOptions.HTTP_METRICS_HISTOGRAMS_ENABLED);
        this.httpServerSLOs = slo(Configuration.getOptionalKcValue(HttpOptions.HTTP_METRICS_SLOS.getKey()));
    }

    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (isHttpClientRequests(id)) {
            return histogramConfig(httpClientHistograms, httpClientSLOs).merge(config);
        }
        if (isHttpServerRequests(id)) {
            return histogramConfig(httpServerHistograms, httpServerSLOs).merge(config);
        }
        return config;
    }

    private double[] slo(Optional<String> sloOptional) {
        if (sloOptional.isPresent()) {
            double[] sloArray = Arrays.stream(sloOptional.get().split(","))
                  .filter(s -> !s.trim().isEmpty())
                  .mapToDouble(s -> TimeUnit.MILLISECONDS.toNanos(Long.parseLong(s)))
                  .toArray();
            if (sloArray.length == 0) {
                return null;
            }
            return sloArray;
        } else {
            return null;
        }
    }

    private DistributionStatisticConfig histogramConfig(boolean enabled, double... slos) {
        DistributionStatisticConfig.Builder builder = DistributionStatisticConfig.builder()
              .percentilesHistogram(enabled);
        if (slos != null) {
            builder.serviceLevelObjectives(slos);
        }
        return builder.build();
    }

    private boolean isHttpServerRequests(Meter.Id id) {
        return "http.server.requests".equals(id.getName());
    }

    private boolean isHttpClientRequests(Meter.Id id) {
        return "httpcomponents.httpclient.request".equals(id.getName());
    }
}
