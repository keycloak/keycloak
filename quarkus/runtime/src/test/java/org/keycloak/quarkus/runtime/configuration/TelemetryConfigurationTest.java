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

    @Test
    public void logsDefault() {
        initConfig();

        assertConfig(Map.of(
                "telemetry-logs-enabled", "false",
                "telemetry-logs-endpoint", "http://localhost:4317",
                "telemetry-logs-protocol", "grpc",
                "telemetry-logs-level", "all"
        ));

        assertExternalConfig(Map.of(
                "quarkus.otel.logs.enabled", "false",
                "quarkus.otel.enabled", "false",
                "quarkus.otel.exporter.otlp.logs.endpoint", "http://localhost:4317",
                "quarkus.otel.exporter.otlp.logs.protocol", "grpc",
                "quarkus.otel.logs.level","ALL"
        ));
    }

    @Test
    public void logsPriority() {
        ConfigArgsConfigSource.setCliArgs("--features=opentelemetry-logs", "--telemetry-logs-enabled=true", "--telemetry-logs-endpoint=localhost:2000", "--telemetry-logs-protocol=http/protobuf", "--telemetry-logs-level=warn");
        initConfig();
        assertConfig(Map.of(
                "telemetry-logs-enabled", "true",
                "telemetry-logs-endpoint", "localhost:2000",
                "telemetry-logs-protocol", "http/protobuf",
                "telemetry-logs-level", "warn"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.logs.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.logs.endpoint", "localhost:2000",
                "quarkus.otel.exporter.otlp.logs.protocol", "http/protobuf",
                "quarkus.otel.logs.level","WARN"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--features=opentelemetry-logs", "--telemetry-endpoint=http://keycloak.org:1234", "--telemetry-protocol=grpc", "--telemetry-logs-enabled=true", "--telemetry-logs-endpoint=my-domain:2001", "--telemetry-logs-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "telemetry-logs-enabled", "true",
                "telemetry-logs-endpoint", "my-domain:2001",
                "telemetry-logs-protocol", "http/protobuf",
                "telemetry-endpoint", "http://keycloak.org:1234",
                "telemetry-protocol", "grpc"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.logs.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.logs.endpoint", "my-domain:2001",
                "quarkus.otel.exporter.otlp.logs.protocol", "http/protobuf"
        ));
    }

    @Test
    public void metricsDefaults() {
        initConfig();

        assertConfig(Map.of(
                "telemetry-metrics-enabled", "false",
                "telemetry-metrics-endpoint", "http://localhost:4317",
                "telemetry-metrics-protocol", "grpc",
                "telemetry-metrics-interval", "60s"
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
    public void metricsPriorities() {
        ConfigArgsConfigSource.setCliArgs("--features=opentelemetry-metrics", "--metrics-enabled=true", "--telemetry-metrics-enabled=true", "--telemetry-metrics-endpoint=localhost:2000", "--telemetry-metrics-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "telemetry-metrics-enabled", "true",
                "telemetry-metrics-endpoint", "localhost:2000",
                "telemetry-metrics-protocol", "http/protobuf"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.metrics.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.metrics.endpoint", "localhost:2000",
                "quarkus.otel.exporter.otlp.metrics.protocol", "http/protobuf"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--features=opentelemetry-metrics", "--metrics-enabled=true", "--telemetry-endpoint=http://keycloak.org:1234", "--telemetry-protocol=grpc", "--telemetry-metrics-enabled=true", "--telemetry-metrics-endpoint=my-domain:2001", "--telemetry-metrics-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "telemetry-metrics-enabled", "true",
                "telemetry-metrics-endpoint", "my-domain:2001",
                "telemetry-metrics-protocol", "http/protobuf",
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
