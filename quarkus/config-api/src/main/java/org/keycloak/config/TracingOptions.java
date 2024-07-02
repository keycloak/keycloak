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

package org.keycloak.config;

import io.quarkus.opentelemetry.runtime.config.build.SamplerType;

import java.util.Arrays;

public class TracingOptions {

    public static final Option<Boolean> TRACING_ENABLED = new OptionBuilder<>("tracing-enabled", Boolean.class)
            .category(OptionCategory.TRACING)
            .description("Enables the OpenTelemetry tracing.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> TRACING_ENDPOINT = new OptionBuilder<>("tracing-endpoint", String.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry endpoint to connect to.")
            .defaultValue("http://localhost:4317")
            .build();

    public static final Option<String> TRACING_PROTOCOL = new OptionBuilder<>("tracing-protocol", String.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry protocol used for the telemetry data.")
            .defaultValue("grpc")
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<String> TRACING_SAMPLER_TYPE = new OptionBuilder<>("tracing-sampler-type", String.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry sampler to use for tracing.")
            .defaultValue(SamplerType.TRACE_ID_RATIO.getValue()) // TODO discuss what would be the most suitable here
            .expectedValues(Arrays.stream(SamplerType.values()).map(SamplerType::getValue).toList())
            .buildTime(true)
            .build();

    public static final Option<Double> TRACING_SAMPLER_RATIO = new OptionBuilder<>("tracing-sampler-ratio", Double.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry sampler ratio. Probability that a span will be sampled. Expected double value in interval <0,1).")
            .defaultValue(1.0d)
            .build();

    public static final Option<Boolean> TRACING_JDBC_ENABLED = new OptionBuilder<>("tracing-jdbc-enabled", Boolean.class)
            .category(OptionCategory.TRACING)
            .description("(Preview) Enables the OpenTelemetry JDBC tracing.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();
}
