/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.configuration;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.config.LoggingOptions;
import org.keycloak.config.TracingOptions;

import io.quarkus.opentelemetry.runtime.config.build.SamplerType;
import org.junit.Test;

public class TracingConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void defaultValues() {
        initConfig();

        assertConfig(Map.of(
                "tracing-enabled", "false",
                "tracing-endpoint", "http://localhost:4317",
                "tracing-protocol", "grpc",
                "tracing-jdbc-enabled", "false",
                "tracing-sampler-type", SamplerType.TRACE_ID_RATIO.getValue(),
                "tracing-sampler-ratio", "1.0",
                "tracing-compression", TracingOptions.TracingCompression.none.name(),
                "log-console-include-trace", "true",
                "log-file-include-trace", "true",
                "log-syslog-include-trace", "true"
        ));
        assertConfig("tracing-service-name", "keycloak");

        assertExternalConfig(Map.of(
                "quarkus.otel.enabled", "false",
                "quarkus.otel.service.name", "keycloak",
                "quarkus.otel.exporter.otlp.traces.endpoint", "http://localhost:4317",
                "quarkus.otel.exporter.otlp.traces.protocol", "grpc",
                "quarkus.datasource.jdbc.telemetry", "false",
                "quarkus.otel.traces.sampler", SamplerType.TRACE_ID_RATIO.getValue(),
                "quarkus.otel.traces.sampler.arg", "1.0",
                "quarkus.otel.exporter.otlp.traces.compression", TracingOptions.TracingCompression.none.name()
        ));
    }

    @Test
    public void differentValues() {
        putEnvVars(Map.of(
                "KC_TRACING_ENABLED", "true",
                "KC_TRACING_ENDPOINT", "http://something:4444",
                "KC_TRACING_PROTOCOL", "http/protobuf",
                "KC_TRACING_JDBC_ENABLED", "false",
                "KC_TRACING_SAMPLER_TYPE", SamplerType.PARENT_BASED_ALWAYS_ON.getValue(),
                "KC_TRACING_SAMPLER_RATIO", "0.5",
                "KC_TRACING_COMPRESSION", TracingOptions.TracingCompression.gzip.name(),
                "KC_LOG_CONSOLE_INCLUDE_TRACE", "false",
                "KC_LOG_FILE_INCLUDE_TRACE", "false",
                "KC_LOG_SYSLOG_INCLUDE_TRACE", "false"
        ));
        putEnvVars(Map.of(
                "KC_TRACING_SERVICE_NAME", "keycloak-42",
                "KC_TRACING_RESOURCE_ATTRIBUTES", "host.name=unknown,service.version=30"
        ));

        initConfig();

        assertConfig(Map.of(
                "tracing-enabled", "true",
                "tracing-endpoint", "http://something:4444",
                "tracing-protocol", "http/protobuf",
                "tracing-jdbc-enabled", "false",
                "tracing-sampler-type", SamplerType.PARENT_BASED_ALWAYS_ON.getValue(),
                "tracing-sampler-ratio", "0.5",
                "tracing-compression", TracingOptions.TracingCompression.gzip.name(),
                "tracing-service-name", "keycloak-42",
                "tracing-resource-attributes", "host.name=unknown,service.version=30"
        ));
        assertConfig(Map.of(
                "log-console-include-trace", "false",
                "log-file-include-trace", "false",
                "log-syslog-include-trace", "false"
        ));

        assertExternalConfig(Map.of(
                "quarkus.otel.enabled", "true",
                "quarkus.otel.exporter.otlp.traces.endpoint", "http://something:4444",
                "quarkus.otel.exporter.otlp.traces.protocol", "http/protobuf",
                "quarkus.datasource.jdbc.telemetry", "false",
                "quarkus.otel.traces.sampler", SamplerType.PARENT_BASED_ALWAYS_ON.getValue(),
                "quarkus.otel.traces.sampler.arg", "0.5",
                "quarkus.otel.exporter.otlp.traces.compression", TracingOptions.TracingCompression.gzip.name(),
                "quarkus.otel.service.name", "keycloak-42",
                "quarkus.otel.resource.attributes", "host.name=unknown,service.version=30"
        ));
    }

    @Test
    public void serviceNamePreference() {
        putEnvVars(Map.of(
                "KC_TRACING_ENABLED", "true",
                "KC_TRACING_SERVICE_NAME", "service-name",
                "KC_TRACING_RESOURCE_ATTRIBUTES", "service.name=new-service-name"
        ));

        initConfig();

        assertConfig(Map.of(
                "tracing-enabled", "true",
                "tracing-service-name", "service-name",
                "tracing-resource-attributes", "service.name=new-service-name"
        ));

        assertExternalConfig("quarkus.otel.service.name", "service-name");
    }

    @Test
    public void serviceNameResourceAttributes() {
        putEnvVars(Map.of(
                "KC_TRACING_ENABLED", "true",
                "KC_TRACING_RESOURCE_ATTRIBUTES", "service.name=new-service-name"
        ));

        initConfig();

        assertConfig(Map.of(
                "tracing-enabled", "true",
                "tracing-resource-attributes", "service.name=new-service-name"
        ));

        // the default value should be used
        assertExternalConfig("quarkus.otel.service.name", "keycloak");
    }

    @Test
    public void consoleLogTraceOn() {
        assertLogFormat(LoggingOptions.Handler.console, true, false, LoggingOptions.DEFAULT_LOG_FORMAT_FUNC.apply("traceId=%X{traceId}, parentId=%X{parentId}, spanId=%X{spanId}, sampled=%X{sampled} "));
    }

    @Test
    public void consoleLogMdcOn() {
        assertLogFormat(LoggingOptions.Handler.console, true, true, LoggingOptions.DEFAULT_LOG_FORMAT_FUNC.apply("%X "));
    }

    @Test
    public void consoleLogTraceOff() {
        assertLogFormat(LoggingOptions.Handler.console, false, false, LoggingOptions.DEFAULT_LOG_FORMAT);
    }

    @Test
    public void fileLogTraceOn() {
        assertLogFormat(LoggingOptions.Handler.file, true, false, LoggingOptions.DEFAULT_LOG_FORMAT_FUNC.apply("traceId=%X{traceId}, parentId=%X{parentId}, spanId=%X{spanId}, sampled=%X{sampled} "));
    }

    @Test
    public void fileLogMdcOn() {
        assertLogFormat(LoggingOptions.Handler.file, true, true, LoggingOptions.DEFAULT_LOG_FORMAT_FUNC.apply("%X "));
    }

    @Test
    public void fileLogTraceOff() {
        assertLogFormat(LoggingOptions.Handler.file, false, false, LoggingOptions.DEFAULT_LOG_FORMAT);
    }

    @Test
    public void syslogLogTraceOn() {
        assertLogFormat(LoggingOptions.Handler.syslog, true, false, LoggingOptions.DEFAULT_LOG_FORMAT_FUNC.apply("traceId=%X{traceId}, parentId=%X{parentId}, spanId=%X{spanId}, sampled=%X{sampled} "));
    }

    @Test
    public void syslogLogMdcOn() {
        assertLogFormat(LoggingOptions.Handler.syslog, true, true, LoggingOptions.DEFAULT_LOG_FORMAT_FUNC.apply("%X "));
    }

    @Test
    public void syslogLogTraceOff() {
        assertLogFormat(LoggingOptions.Handler.syslog, false, false, LoggingOptions.DEFAULT_LOG_FORMAT);
    }

    /**
     * Assert log format for individual log handlers with different `includeTrace` option.
     * It also checks the log format is unchanged despite the `includeTrace` option, when explicitly specified.
     */
    protected void assertLogFormat(LoggingOptions.Handler loggerType, boolean includeTrace, boolean includeMdc, String expectedFormat) {
        var envVars = new HashMap<String, String>();
        envVars.put("KC_TRACING_ENABLED", "true");
        if (includeMdc) {
            envVars.put("KC_LOG_MDC_ENABLED", "true");
        }
        envVars.put("KC_LOG_" + loggerType.name().toUpperCase() + "_INCLUDE_TRACE", Boolean.toString(includeTrace));
        envVars.put("KC_LOG_" + loggerType.name().toUpperCase() + "_INCLUDE_MDC", Boolean.toString(includeMdc));

        putEnvVars(envVars);

        initConfig();

        assertConfig(Map.of(
                "tracing-enabled", "true",
                "log-" + loggerType.name() + "-include-trace", Boolean.toString(includeTrace)
        ));

        assertExternalConfig(Map.of(
                "quarkus.otel.enabled", "true",
                "quarkus.log." + loggerType.name() + ".format", expectedFormat
        ));

        // Assert no effect on the format when explicitly set
        envVars.put("KC_LOG_" + loggerType.name().toUpperCase() + "_FORMAT", "someFormat");
        putEnvVars(envVars);

        initConfig();

        assertConfig(Map.of(
                "tracing-enabled", "true",
                "log-" + loggerType.name() + "-include-trace", Boolean.toString(includeTrace)
        ));

        assertExternalConfig(Map.of(
                "quarkus.otel.enabled", "true",
                "quarkus.log." + loggerType.name() + ".format", "someFormat"
        ));
    }
}
