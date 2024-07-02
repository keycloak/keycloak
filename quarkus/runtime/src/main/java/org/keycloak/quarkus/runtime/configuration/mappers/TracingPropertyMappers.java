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

package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigValue;
import org.keycloak.config.TracingOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class TracingPropertyMappers {
    private static final String TRACING_ENABLED_MSG = "Tracing is enabled";

    private TracingPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[]{
                fromOption(TracingOptions.TRACING_ENABLED)
                        .to("quarkus.otel.traces.enabled")
                        .build(),
                fromOption(TracingOptions.TRACING_ENDPOINT)
                        .isEnabled(TracingPropertyMappers::isTracingEnabled, TRACING_ENABLED_MSG)
                        .to("quarkus.otel.exporter.otlp.endpoint")
                        .paramLabel("url")
                        .validator(TracingPropertyMappers::validateEndpoint)
                        .build(),
                fromOption(TracingOptions.TRACING_PROTOCOL)
                        .isEnabled(TracingPropertyMappers::isTracingEnabled, TRACING_ENABLED_MSG)
                        .to("quarkus.otel.exporter.otlp.traces.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(TracingOptions.TRACING_SAMPLER_TYPE)
                        .isEnabled(TracingPropertyMappers::isTracingEnabled, TRACING_ENABLED_MSG)
                        .to("quarkus.otel.traces.sampler")
                        .paramLabel("type")
                        .build(),
                fromOption(TracingOptions.TRACING_SAMPLER_RATIO)
                        .isEnabled(TracingPropertyMappers::isTracingEnabled, TRACING_ENABLED_MSG)
                        .to("quarkus.otel.traces.sampler.arg")
                        .validator(TracingPropertyMappers::validateRatio)
                        .paramLabel("ratio")
                        .build(),
                fromOption(TracingOptions.TRACING_JDBC_ENABLED)
                        .isEnabled(TracingPropertyMappers::isTracingEnabled, TRACING_ENABLED_MSG)
                        .to("quarkus.datasource.jdbc.telemetry")
                        .build()
        };
    }

    private static void validateEndpoint(PropertyMapper<String> mapper, ConfigValue value) {
        if (value == null || StringUtil.isBlank(value.getValue())) {
            throw new PropertyException("URL specified in 'tracing-endpoint' option must not be empty.");
        }

        if (!isValidUrl(value.getValue())) {
            throw new PropertyException("URL specified in 'tracing-endpoint' option is invalid.");
        }
    }

    private static void validateRatio(PropertyMapper<Double> mapper, ConfigValue value) {
        if (value == null || StringUtil.isBlank(value.getValue())) {
            throw new PropertyException("Ratio in 'tracing-sampler-ratio' option must not be empty.");
        }

        try {
            var ratio = Double.parseDouble(value.getValue());
            if (ratio <= 0.0 || ratio > 1.0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new PropertyException("Ratio in 'tracing-sampler-ratio' option must be a double value in interval <0,1).");
        }
    }

    public static boolean isTracingEnabled() {
        return Configuration.isTrue(TracingOptions.TRACING_ENABLED);
    }

    public static boolean isTracingJdbcEnabled() {
        return Configuration.isTrue(TracingOptions.TRACING_JDBC_ENABLED);
    }

    private static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}
