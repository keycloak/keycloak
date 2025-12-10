package org.keycloak.quarkus.runtime.configuration;

import java.util.Map;

import org.junit.Test;

public class TelemetryConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void rootDefaults() {
        initConfig();
        assertConfig(Map.of(
                "telemetry-enabled", "false",
                "telemetry-endpoint", "http://localhost:4317",
                "telemetry-service-name", "keycloak",
                "telemetry-protocol", "grpc"
        ));
        assertConfigNull("telemetry-resource-attributes");
        assertExternalConfig(Map.of(
                "quarkus.otel.enabled", "false",
                "quarkus.otel.exporter.otlp.endpoint", "http://localhost:4317",
                "quarkus.otel.service.name", "keycloak",
                "quarkus.otel.exporter.otlp.traces.protocol", "grpc"
        ));
        assertExternalConfigNull("quarkus.otel.resource.attributes");
    }

    @Test
    public void tracesDeprecated() {
        // propagate to Quarkus props
        ConfigArgsConfigSource.setCliArgs("--tracing-service-name=something", "--tracing-resource-attributes=val1=hello");
        initConfig();
        assertConfig(Map.of(
                "tracing-service-name", "something",
                "tracing-resource-attributes", "val1=hello"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.service.name", "something",
                "quarkus.otel.resource.attributes", "val1=hello"
        ));
        onAfter();

        // use recommended OTel properties
        ConfigArgsConfigSource.setCliArgs("--telemetry-service-name=something2", "--telemetry-resource-attributes=val2=hello2");
        initConfig();
        assertConfig(Map.of(
                "telemetry-service-name", "something2",
                "telemetry-resource-attributes", "val2=hello2"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.service.name", "something2",
                "quarkus.otel.resource.attributes", "val2=hello2"
        ));
        onAfter();

        // check priority of options
        ConfigArgsConfigSource.setCliArgs("--telemetry-service-name=something3", "--telemetry-resource-attributes=val3=hello3",
                "--tracing-service-name=something", "--tracing-resource-attributes=val1=hello");
        initConfig();
        assertConfig(Map.of(
                "telemetry-service-name", "something3",
                "telemetry-resource-attributes", "val3=hello3",
                "tracing-service-name", "something",
                "tracing-resource-attributes", "val1=hello"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.service.name", "something3",
                "quarkus.otel.resource.attributes", "val3=hello3"
        ));
    }
}
