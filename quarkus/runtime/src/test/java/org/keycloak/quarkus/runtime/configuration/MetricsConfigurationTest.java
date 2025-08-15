package org.keycloak.quarkus.runtime.configuration;

import org.junit.Test;

import java.util.Map;

public class MetricsConfigurationTest extends AbstractConfigurationTest{

    @Test
    public void defaults() {
        initConfig();

        assertConfig(Map.of(
                "metrics-export-enabled", "false",
                "metrics-export-endpoint", "http://localhost:4317",
                "metrics-export-protocol", "grpc",
                "metrics-export-interval", "60s"
        ));

        assertExternalConfig(Map.of(
                "quarkus.otel.metrics.enabled", "false",
                "quarkus.otel.enabled", "false",
                "quarkus.otel.exporter.otlp.metrics.endpoint", "http://localhost:4317",
                "quarkus.otel.exporter.otlp.metrics.protocol", "grpc",
                "quarkus.otel.metric.export.interval", "60s"
        ));
    }

    @Test
    public void priorities() {
        ConfigArgsConfigSource.setCliArgs("--metrics-export-enabled=true", "--metrics-export-endpoint=localhost:2000", "--metrics-export-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "metrics-export-enabled", "true",
                "metrics-export-endpoint", "localhost:2000",
                "metrics-export-protocol", "http/protobuf"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.metrics.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.metrics.endpoint", "localhost:2000",
                "quarkus.otel.exporter.otlp.metrics.protocol", "http/protobuf"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--telemetry-endpoint=http://keycloak.org:1234", "--telemetry-protocol=grpc", "--metrics-export-enabled=true", "--metrics-export-endpoint=my-domain:2001", "--metrics-export-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "metrics-export-enabled", "true",
                "metrics-export-endpoint", "my-domain:2001",
                "metrics-export-protocol", "http/protobuf",
                "telemetry-endpoint", "http://keycloak.org:1234",
                "telemetry-protocol", "grpc"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.metrics.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.metrics.endpoint", "my-domain:2001",
                "quarkus.otel.exporter.otlp.metrics.protocol", "http/protobuf"
        ));
    }

}
