package org.keycloak.config;

public class MetricsOptions {

    public static final Option<Boolean> METRICS_ENABLED = new OptionBuilder<>("metrics-enabled", Boolean.class)
            .category(OptionCategory.METRICS)
            .description("If the server should expose metrics. If enabled, metrics are available at the '/metrics' endpoint.")
            .buildTime(true)
            .defaultValue(Boolean.FALSE)
            .build();

    // Export
    public static final Option<Boolean> METRICS_EXPORT_ENABLED = new OptionBuilder<>("metrics-export-enabled", Boolean.class)
            .category(OptionCategory.METRICS)
            .description("PREVIEW. Enables exporting metrics to a destination handling telemetry data (OpenTelemetry Metrics).")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    public static final Option<String> METRICS_EXPORT_ENDPOINT = new OptionBuilder<>("metrics-export-endpoint", String.class)
            .category(OptionCategory.METRICS)
            .description("PREVIEW.Telemetry (OpenTelemetry) endpoint to connect to for Metrics. If not given, the value is inherited from the '%s' option.".formatted(TelemetryOptions.TELEMETRY_ENDPOINT.getKey()))
            .build();

    public static final Option<String> METRICS_EXPORT_PROTOCOL = new OptionBuilder<>("metrics-export-protocol", String.class)
            .category(OptionCategory.METRICS)
            .description("PREVIEW. Telemetry (OpenTelemetry) protocol used for the metrics telemetry data. If not given, the value is inherited from the '%s' option.".formatted(TelemetryOptions.TELEMETRY_PROTOCOL.getKey()))
            .expectedValues("grpc", "http/protobuf")
            .build();

    public static final Option<String> METRICS_EXPORT_INTERVAL = new OptionBuilder<>("metrics-export-interval", String.class)
            .category(OptionCategory.METRICS)
            .description("PREVIEW. The interval between the start of two metric export attempts to the destination handling telemetry data (OpenTelemetry Metrics). It accepts simplified format for time units as java.time.Duration (like 5000ms, 30s, 5m, 1h). If the value is only a number, it represents time in seconds.")
            .defaultValue("60s")
            .build();
}
