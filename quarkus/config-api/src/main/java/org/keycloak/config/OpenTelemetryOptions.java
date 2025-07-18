package org.keycloak.config;

public class OpenTelemetryOptions {

    public static final Option<Boolean> OTEL_ENABLED = new OptionBuilder<>("opentelemetry-enabled", Boolean.class)
            .category(OptionCategory.OPENTELEMETRY)
            .buildTime(true)
            .hidden()
            .build();

    public static final Option<String> OTEL_ENDPOINT = new OptionBuilder<>("opentelemetry-endpoint", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .defaultValue("http://localhost:4317")
            .build();

    public static final Option<String> OTEL_SERVICE_NAME = new OptionBuilder<>("opentelemetry-service-name", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry service name. Takes precedence over 'service.name' defined in the 'tracing-resource-attributes' property.")
            .defaultValue("keycloak")
            .build();

    public static final Option<String> OTEL_PROTOCOL = new OptionBuilder<>("opentelemetry-protocol", String.class)
            .category(OptionCategory.OPENTELEMETRY)
            .description("OpenTelemetry protocol used for the telemetry data.")
            .defaultValue("grpc")
            .expectedValues("grpc", "http/protobuf")
            .build();
}
