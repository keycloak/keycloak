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

    // Logs
    public static final Option<Boolean> OTEL_LOGS_ENABLED = new OptionBuilder<>("otel-logs-enabled", Boolean.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("PREVIEW. Enables the OpenTelemetry Logs.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> OTEL_LOGS_ENDPOINT = new OptionBuilder<>("otel-logs-endpoint", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("PREVIEW. OpenTelemetry endpoint to connect to for Logs. If not given, the value is inherited from the '%s' option.".formatted(OTEL_ENDPOINT.getKey()))
            .build();

    public static final Option<String> OTEL_LOGS_PROTOCOL = new OptionBuilder<>("otel-logs-protocol", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("PREVIEW. OpenTelemetry protocol used for the logging telemetry data. If not given, the value is inherited from the '%s' option.".formatted(OTEL_PROTOCOL.getKey()))
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<LoggingOptions.Level> OTEL_LOGS_LEVEL = new OptionBuilder<>("otel-logs-level", LoggingOptions.Level.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("Most verbose log level for logs exported to the OpenTelemetry collector. For more information, check the OpenTelemetry guide.")
            .defaultValue(LoggingOptions.Level.ALL)
            .caseInsensitiveExpectedValues(true)
            .build();

    // Metrics
    public static final Option<Boolean> OTEL_METRICS_ENABLED = new OptionBuilder<>("otel-metrics-enabled", Boolean.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("PREVIEW. Enables the OpenTelemetry Metrics (Micrometer to OpenTelemetry bridge).")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> OTEL_METRICS_ENDPOINT = new OptionBuilder<>("otel-metrics-endpoint", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("PREVIEW. OpenTelemetry endpoint to connect to for Metrics. If not given, the value is inherited from the '%s' option.".formatted(OTEL_ENDPOINT.getKey()))
            .build();

    public static final Option<String> OTEL_METRICS_PROTOCOL = new OptionBuilder<>("otel-metrics-protocol", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("PREVIEW. OpenTelemetry protocol used for the metrics telemetry data. If not given, the value is inherited from the '%s' option.".formatted(OTEL_PROTOCOL.getKey()))
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<String> OTEL_METRICS_EXPORT_INTERVAL = new OptionBuilder<>("otel-metrics-export-interval", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("PREVIEW. The interval between the start of two metric export attempts to the OpenTelemetry. It accepts simplified format for time units as java.time.Duration (like 5000ms, 30s, 5m, 1h). If the value is only a number, it represents time in seconds.")
            .defaultValue("60s")
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
}
