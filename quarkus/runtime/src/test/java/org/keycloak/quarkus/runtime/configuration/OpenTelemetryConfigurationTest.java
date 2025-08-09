package org.keycloak.quarkus.runtime.configuration;

import org.junit.Test;

import java.util.Map;

public class OpenTelemetryConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void rootDefaults() {
        initConfig();
        assertConfig(Map.of(
                "otel-enabled", "false",
                "otel-endpoint", "http://localhost:4317",
                "otel-service-name", "keycloak",
                "otel-protocol", "grpc"
        ));
        assertConfigNull("otel-resource-attributes");
        assertExternalConfig(Map.of(
                "quarkus.otel.enabled", "false",
                "quarkus.otel.exporter.otlp.endpoint", "http://localhost:4317",
                "quarkus.otel.service.name", "keycloak",
                "quarkus.otel.exporter.otlp.traces.protocol", "grpc"
        ));
        assertExternalConfigNull("quarkus.otel.resource.attributes");
    }

    @Test
    public void tracesDefault() {
        initConfig();

        assertConfig(Map.of(
                "otel-traces-enabled", "false",
                "otel-traces-endpoint", "http://localhost:4317",
                "otel-traces-protocol", "grpc"
        ));

        assertExternalConfig(Map.of(
                "quarkus.otel.traces.enabled", "false",
                "quarkus.otel.enabled", "false",
                "quarkus.otel.exporter.otlp.traces.endpoint", "http://localhost:4317",
                "quarkus.otel.exporter.otlp.traces.protocol", "grpc"
        ));
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
        ConfigArgsConfigSource.setCliArgs("--otel-service-name=something2", "--otel-resource-attributes=val2=hello2");
        initConfig();
        assertConfig(Map.of(
                "otel-service-name", "something2",
                "otel-resource-attributes", "val2=hello2"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.service.name", "something2",
                "quarkus.otel.resource.attributes", "val2=hello2"
        ));
        onAfter();

        // check priority of options
        ConfigArgsConfigSource.setCliArgs("--otel-service-name=something3", "--otel-resource-attributes=val3=hello3",
                "--tracing-service-name=something", "--tracing-resource-attributes=val1=hello");
        initConfig();
        assertConfig(Map.of(
                "otel-service-name", "something3",
                "otel-resource-attributes", "val3=hello3",
                "tracing-service-name", "something",
                "tracing-resource-attributes", "val1=hello"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.service.name", "something3",
                "quarkus.otel.resource.attributes", "val3=hello3"
        ));
    }

    @Test
    public void tracesPriority() {
        // use OTel options
        ConfigArgsConfigSource.setCliArgs("--otel-traces-enabled=true", "--otel-traces-endpoint=localhost:2000", "--otel-traces-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "otel-traces-enabled", "true",
                "otel-traces-endpoint", "localhost:2000",
                "otel-traces-protocol", "http/protobuf"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.traces.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.traces.endpoint", "localhost:2000",
                "quarkus.otel.exporter.otlp.traces.protocol", "http/protobuf"
        ));
        onAfter();

        // use tracing options
        ConfigArgsConfigSource.setCliArgs("--tracing-enabled=true", "--tracing-endpoint=localhost:2001", "--tracing-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "tracing-enabled", "true",
                "tracing-endpoint", "localhost:2001",
                "tracing-protocol", "http/protobuf",
                "otel-traces-enabled", "false",
                "otel-traces-endpoint", "http://localhost:4317",
                "otel-traces-protocol", "grpc"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.traces.enabled", "true",
                "quarkus.otel.exporter.otlp.traces.endpoint", "localhost:2001",
                "quarkus.otel.exporter.otlp.traces.protocol", "http/protobuf"
        ));
        onAfter();

        // prefer tracing options over OTel options for Tracing
        ConfigArgsConfigSource.setCliArgs("--tracing-enabled=true", "--tracing-endpoint=tracing:2001", "--tracing-protocol=http/protobuf",
                "--otel-traces-enabled=false", "--otel-traces-endpoint=my-domain:2000", "--otel-traces-protocol=grpc");
        initConfig();
        assertConfig(Map.of(
                "tracing-enabled", "true",
                "tracing-endpoint", "tracing:2001",
                "tracing-protocol", "http/protobuf",
                "otel-traces-enabled", "false",
                "otel-traces-endpoint", "my-domain:2000",
                "otel-traces-protocol", "grpc"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.traces.enabled", "true", // 'tracing-enabled' takes precedence
                "quarkus.otel.exporter.otlp.traces.endpoint", "tracing:2001", // 'tracing-endpoint' takes precedence
                "quarkus.otel.exporter.otlp.traces.protocol", "http/protobuf" // 'tracing-protocol' takes precedence
        ));
    }

    @Test
    public void logsDefault() {
        initConfig();

        assertConfig(Map.of(
                "otel-logs-enabled", "false",
                "otel-logs-endpoint", "http://localhost:4317",
                "otel-logs-protocol", "grpc",
                "otel-logs-level", "all"
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
        ConfigArgsConfigSource.setCliArgs("--otel-logs-enabled=true", "--otel-logs-endpoint=localhost:2000", "--otel-logs-protocol=http/protobuf","--otel-logs-level=warn");
        initConfig();
        assertConfig(Map.of(
                "otel-logs-enabled", "true",
                "otel-logs-endpoint", "localhost:2000",
                "otel-logs-protocol", "http/protobuf",
                "otel-logs-level", "warn"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.logs.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.logs.endpoint", "localhost:2000",
                "quarkus.otel.exporter.otlp.logs.protocol", "http/protobuf",
                "quarkus.otel.logs.level","WARN"
        ));
        onAfter();

        ConfigArgsConfigSource.setCliArgs("--otel-endpoint=http://keycloak.org:1234", "--otel-protocol=grpc", "--otel-logs-enabled=true", "--otel-logs-endpoint=my-domain:2001", "--otel-logs-protocol=http/protobuf");
        initConfig();
        assertConfig(Map.of(
                "otel-logs-enabled", "true",
                "otel-logs-endpoint", "my-domain:2001",
                "otel-logs-protocol", "http/protobuf",
                "otel-endpoint", "http://keycloak.org:1234",
                "otel-protocol", "grpc"
        ));
        assertExternalConfig(Map.of(
                "quarkus.otel.logs.enabled", "true",
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.logs.endpoint", "my-domain:2001",
                "quarkus.otel.exporter.otlp.logs.protocol", "http/protobuf"
        ));
    }
}
