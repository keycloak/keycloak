package org.keycloak.config;

import java.util.List;

public class TelemetryOptions {

    public static final Option<Boolean> TELEMETRY_ENABLED = new OptionBuilder<>("telemetry-enabled", Boolean.class)
            .category(OptionCategory.TELEMETRY)
            .buildTime(true)
            .hidden()
            .build();

    public static final Option<String> TELEMETRY_ENDPOINT = new OptionBuilder<>("telemetry-endpoint", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry endpoint to connect to.")
            .defaultValue("http://localhost:4317")
            .build();

    public static final Option<String> TELEMETRY_SERVICE_NAME = new OptionBuilder<>("telemetry-service-name", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry service name. Takes precedence over 'service.name' defined in the 'telemetry-resource-attributes' property.")
            .defaultValue("keycloak")
            .build();

    public static final Option<String> TELEMETRY_PROTOCOL = new OptionBuilder<>("telemetry-protocol", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry protocol used for the communication between server and OpenTelemetry collector.")
            .defaultValue("grpc")
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<List<String>> TELEMETRY_RESOURCE_ATTRIBUTES = OptionBuilder.listOptionBuilder("telemetry-resource-attributes", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry resource attributes characterize the telemetry producer. Values in format 'key1=val1,key2=val2'.")
            .build();

    public static final Option<String> TELEMETRY_HEADER = new OptionBuilder<>("telemetry-header-<header>", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("General OpenTelemetry header that will be part of the exporter request (mainly useful for providing Authorization header). Check the documentation on how to set environment variables for headers containing special characters or custom case-sensitive headers.")
            .build();

    // Telemetry Logs
    public static final Option<Boolean> TELEMETRY_LOGS_ENABLED = new OptionBuilder<>("telemetry-logs-enabled", Boolean.class)
            .category(OptionCategory.TELEMETRY)
            .description("Enables exporting logs to a destination handling OpenTelemetry logs.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> TELEMETRY_LOGS_ENDPOINT = new OptionBuilder<>("telemetry-logs-endpoint", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry endpoint to export logs to. If not given, the value is inherited from the '%s' option.".formatted(TELEMETRY_ENDPOINT.getKey()))
            .build();

    public static final Option<String> TELEMETRY_LOGS_PROTOCOL = new OptionBuilder<>("telemetry-logs-protocol", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry protocol used for exporting logs. If not given, the value is inherited from the '%s' option.".formatted(TELEMETRY_PROTOCOL.getKey()))
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<LoggingOptions.Level> TELEMETRY_LOGS_LEVEL = new OptionBuilder<>("telemetry-logs-level", LoggingOptions.Level.class)
            .category(OptionCategory.TELEMETRY)
            .description("The most verbose log level exported to the telemetry endpoint. For more information, check the Telemetry guide.")
            .defaultValue(LoggingOptions.Level.ALL)
            .caseInsensitiveExpectedValues(true)
            .build();

    public static final Option<String> TELEMETRY_LOGS_HEADER = new OptionBuilder<>("telemetry-logs-header-<header>", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry header that will be part of the log exporter request (mainly useful for providing Authorization header). Check the documentation on how to set environment variables for headers containing special characters or custom case-sensitive headers.")
            .build();

    public static final Option<List<String>> TELEMETRY_LOGS_HEADERS = OptionBuilder.listOptionBuilder("telemetry-logs-headers", String.class)
            .category(OptionCategory.TELEMETRY)
            .hidden()
            .description("Hidden option for OpenTelemetry headers that will be part of the exporter request. Values in format 'key1=val1,key2=val2'. Overrides the 'telemetry-logs-header-<header>' options.")
            .build();

    // Telemetry Metrics
    public static final Option<Boolean> TELEMETRY_METRICS_ENABLED = new OptionBuilder<>("telemetry-metrics-enabled", Boolean.class)
            .category(OptionCategory.TELEMETRY)
            .description("Enables exporting metrics to a destination handling OpenTelemetry metrics.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> TELEMETRY_METRICS_ENDPOINT = new OptionBuilder<>("telemetry-metrics-endpoint", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry endpoint to connect to for Metrics. If not given, the value is inherited from the '%s' option.".formatted(TelemetryOptions.TELEMETRY_ENDPOINT.getKey()))
            .build();

    public static final Option<String> TELEMETRY_METRICS_PROTOCOL = new OptionBuilder<>("telemetry-metrics-protocol", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry protocol used for the metrics telemetry data. If not given, the value is inherited from the '%s' option.".formatted(TelemetryOptions.TELEMETRY_PROTOCOL.getKey()))
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<String> TELEMETRY_METRICS_INTERVAL = new OptionBuilder<>("telemetry-metrics-interval", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("The interval between the start of two metric export attempts to the destination handling OpenTelemetry metrics data. It accepts simplified format for time units as java.time.Duration (like 5000ms, 30s, 5m, 1h). If the value is only a number, it represents time in seconds.")
            .defaultValue("60s")
            .build();

    public static final Option<String> TELEMETRY_METRICS_HEADER = new OptionBuilder<>("telemetry-metrics-header-<header>", String.class)
            .category(OptionCategory.TELEMETRY)
            .description("OpenTelemetry header that will be part of the metrics exporter request (mainly useful for providing Authorization header). Check the documentation on how to set environment variables for headers containing special characters or custom case-sensitive headers.")
            .build();

    public static final Option<List<String>> TELEMETRY_METRICS_HEADERS = OptionBuilder.listOptionBuilder("telemetry-metrics-headers", String.class)
            .category(OptionCategory.TELEMETRY)
            .hidden()
            .description("Hidden option for OpenTelemetry headers that will be part of the exporter request. Values in format 'key1=val1,key2=val2'. Overrides the 'telemetry-metrics-header-<header>' options.")
            .build();
}
