package org.keycloak.config;

import java.util.List;

public class OpenTelemetryOptions {

    public static final Option<Boolean> OTEL_ENABLED = new OptionBuilder<>("otel-enabled", Boolean.class)
            .category(OptionCategory.OPENTELEMETRY)
            .buildTime(true)
            .hidden()
            .build();

    public static final Option<String> OTEL_ENDPOINT = new OptionBuilder<>("otel-endpoint", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry endpoint to connect to.")
            .defaultValue("http://localhost:4317")
            .build();

    public static final Option<String> OTEL_SERVICE_NAME = new OptionBuilder<>("otel-service-name", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry service name. Takes precedence over 'service.name' defined in the 'otel-resource-attributes' property.")
            .defaultValue("keycloak")
            .build();

    public static final Option<String> OTEL_PROTOCOL = new OptionBuilder<>("otel-protocol", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry protocol used for the telemetry data.")
            .defaultValue("grpc")
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<List<String>> OTEL_RESOURCE_ATTRIBUTES = OptionBuilder.listOptionBuilder("otel-resource-attributes", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry resource attributes characterize the telemetry producer. Values in format 'key1=val1,key2=val2'.")
            .build();

    // Traces
    public static final Option<Boolean> OTEL_TRACES_ENABLED = new OptionBuilder<>("otel-traces-enabled", Boolean.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("Enables the OpenTelemetry Traces. Property 'tracing-enabled' takes precedence.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> OTEL_TRACES_ENDPOINT = new OptionBuilder<>("otel-traces-endpoint", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry endpoint to connect to for Traces. If not given, the value is inherited from the '%s' option. Property 'tracing-endpoint' takes precedence.".formatted(OTEL_ENDPOINT.getKey()))
            .build();

    public static final Option<String> OTEL_TRACES_PROTOCOL = new OptionBuilder<>("otel-traces-protocol", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry protocol used for the tracing telemetry data. If not given, the value is inherited from the '%s' option. Property 'tracing-protocol' takes precedence.".formatted(OTEL_PROTOCOL.getKey()))
            .expectedValues("grpc", "http/protobuf")
            .build();
    // Logs
    // Metrics
}
