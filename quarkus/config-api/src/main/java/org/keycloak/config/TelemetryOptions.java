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
}
