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
import java.util.List;

public class TracingOptions {

    public static final Option<Boolean> TRACING_ENABLED = new OptionBuilder<>("tracing-enabled", Boolean.class)
            .category(OptionCategory.TRACING)
            .description("Enables the OpenTelemetry tracing.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> TRACING_ENDPOINT = new OptionBuilder<>("tracing-endpoint", String.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry endpoint to connect to for traces. If not given, the value is inherited from the '%s' option.".formatted(TelemetryOptions.TELEMETRY_ENDPOINT.getKey()))
            .defaultValue("http://localhost:4317")
            .build();

    public static final Option<Boolean> TRACING_JDBC_ENABLED = new OptionBuilder<>("tracing-jdbc-enabled", Boolean.class)
            .category(OptionCategory.TRACING)
            .description("Enables the OpenTelemetry JDBC tracing.")
            .defaultValue(true)
            .buildTime(true)
            .build();

    /**
     * @deprecated use the {@link TelemetryOptions#TELEMETRY_SERVICE_NAME}
     */
    @Deprecated
    public static final Option<String> TRACING_SERVICE_NAME = new OptionBuilder<>("tracing-service-name", String.class)
            .category(OptionCategory.TRACING)
            .deprecated()
            .deprecatedMetadata(DeprecatedMetadata.deprecateOption("Service name is not directly related to Tracing and you should use the Telemetry option which takes precedence.", TelemetryOptions.TELEMETRY_SERVICE_NAME.getKey()))
            .description("OpenTelemetry service name. Takes precedence over 'service.name' defined in the 'tracing-resource-attributes' property. If not given, the value is inherited from the '%s' option.".formatted(TelemetryOptions.TELEMETRY_SERVICE_NAME.getKey()))
            .defaultValue("keycloak")
            .build();

    /**
     * @deprecated use the {@link TelemetryOptions#TELEMETRY_RESOURCE_ATTRIBUTES}
     */
    @Deprecated
    public static final Option<List<String>> TRACING_RESOURCE_ATTRIBUTES = OptionBuilder.listOptionBuilder("tracing-resource-attributes", String.class)
            .category(OptionCategory.TRACING)
            .deprecated()
            .deprecatedMetadata(DeprecatedMetadata.deprecateOption("Resource attributes are not directly related to Tracing and you should use the Telemetry option which takes precedence.", TelemetryOptions.TELEMETRY_RESOURCE_ATTRIBUTES.getKey()))
            .description("OpenTelemetry resource attributes present in the exported trace to characterize the telemetry producer. Values in format 'key1=val1,key2=val2'. If not given, the value is inherited from the '%s' option. For more information, check the Tracing guide.".formatted(TelemetryOptions.TELEMETRY_RESOURCE_ATTRIBUTES.getKey()))
            .build();

    public static final Option<String> TRACING_PROTOCOL = new OptionBuilder<>("tracing-protocol", String.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry protocol used for the telemetry data. If not given, the value is inherited from the '%s' option.".formatted(TelemetryOptions.TELEMETRY_PROTOCOL.getKey()))
            .defaultValue("grpc")
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<String> TRACING_SAMPLER_TYPE = new OptionBuilder<>("tracing-sampler-type", String.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry sampler to use for tracing.")
            .defaultValue(SamplerType.TRACE_ID_RATIO.getValue())
            .expectedValues(Arrays.stream(SamplerType.values()).map(SamplerType::getValue).toList())
            .buildTime(true)
            .build();

    public static final Option<Double> TRACING_SAMPLER_RATIO = new OptionBuilder<>("tracing-sampler-ratio", Double.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry sampler ratio. Probability that a span will be sampled. Expected double value in interval [0,1].")
            .defaultValue(1.0d)
            .build();

    public enum TracingCompression {
        gzip,
        none
    }

    public static final Option<TracingCompression> TRACING_COMPRESSION = new OptionBuilder<>("tracing-compression", TracingCompression.class)
            .category(OptionCategory.TRACING)
            .description("OpenTelemetry compression method used to compress payloads. If unset, compression is disabled.")
            .defaultValue(TracingCompression.none)
            .build();

    public static final Option<Boolean> TRACING_INFINISPAN_ENABLED = new OptionBuilder<>("tracing-infinispan-enabled", Boolean.class)
            .category(OptionCategory.TRACING)
            .description("Enables the OpenTelemetry tracing for embedded Infinispan.")
            .defaultValue(true)
            .build();

}
