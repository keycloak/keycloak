package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.common.Profile;
import org.keycloak.config.OpenTelemetryOptions;
import org.keycloak.config.TracingOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.keycloak.config.OpenTelemetryOptions.OTEL_ENABLED;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_ENDPOINT;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_LOGS_ENABLED;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_LOGS_ENDPOINT;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_LOGS_LEVEL;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_LOGS_PROTOCOL;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_PROTOCOL;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_RESOURCE_ATTRIBUTES;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_SERVICE_NAME;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_TRACES_ENABLED;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_TRACES_ENDPOINT;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_TRACES_PROTOCOL;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class OpenTelemetryPropertyMappers {
    private static final String OTEL_FEATURE_ENABLED_MSG = "'opentelemetry' feature is enabled";
    private static final String OTEL_COLLECTOR_ENABLED_MSG = "any of available OpenTelemetry components (Logs, Traces) is turned on";

    private static final String OTEL_LOGS_ENABLED_MSG = "OpenTelemetry Logs is enabled";
    private static final String OTEL_TRACES_ENABLED_MSG = "OpenTelemetry Traces is enabled";

    private OpenTelemetryPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[]{
                fromOption(OTEL_ENABLED)
                        .isEnabled(OpenTelemetryPropertyMappers::isFeatureEnabled, OTEL_FEATURE_ENABLED_MSG)
                        .transformer(OpenTelemetryPropertyMappers::checkIfDependantsAreEnabled)
                        .to("quarkus.otel.enabled")
                        .build(),
                fromOption(OTEL_ENDPOINT)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.exporter.otlp.endpoint")
                        .paramLabel("url")
                        .validator(OpenTelemetryPropertyMappers::validateEndpoint)
                        .build(),
                fromOption(OTEL_SERVICE_NAME)
                        .mapFrom(TracingOptions.TRACING_SERVICE_NAME) // the tracing option is deprecated, and we need to get the value
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.service.name")
                        .paramLabel("name")
                        .build(),
                fromOption(OTEL_PROTOCOL)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.exporter.otlp.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(OTEL_RESOURCE_ATTRIBUTES)
                        .mapFrom(TracingOptions.TRACING_RESOURCE_ATTRIBUTES) // the tracing option is deprecated, and we need to get the value
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.resource.attributes")
                        .paramLabel("attributes")
                        .build(),

                // Logs
                fromOption(OTEL_LOGS_ENABLED)
                        .to("quarkus.otel.logs.enabled")
                        .build(),
                fromOption(OTEL_LOGS_ENDPOINT)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryLogsEnabled, OTEL_LOGS_ENABLED_MSG)
                        .mapFrom(OpenTelemetryOptions.OTEL_ENDPOINT)
                        .to("quarkus.otel.exporter.otlp.logs.endpoint")
                        .validator(OpenTelemetryPropertyMappers::validateEndpoint)
                        .paramLabel("url")
                        .build(),
                fromOption(OTEL_LOGS_PROTOCOL)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryLogsEnabled, OTEL_LOGS_ENABLED_MSG)
                        .mapFrom(OpenTelemetryOptions.OTEL_PROTOCOL)
                        .to("quarkus.otel.exporter.otlp.logs.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(OTEL_LOGS_LEVEL)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryLogsEnabled, OTEL_LOGS_ENABLED_MSG)
                        .to("quarkus.otel.logs.level")
                        .paramLabel("level")
                        .transformer(LoggingPropertyMappers::upperCase)
                        .build(),

                // Traces
                fromOption(OTEL_TRACES_ENABLED)
                        .build(),
                fromOption(OTEL_TRACES_ENDPOINT)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryTracesEnabled, OTEL_TRACES_ENABLED_MSG)
                        .mapFrom(OpenTelemetryOptions.OTEL_ENDPOINT)
                        .paramLabel("url")
                        .validator(OpenTelemetryPropertyMappers::validateEndpoint)
                        .build(),
                fromOption(OTEL_TRACES_PROTOCOL)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryTracesEnabled, OTEL_TRACES_ENABLED_MSG)
                        .mapFrom(OpenTelemetryOptions.OTEL_PROTOCOL)
                        .paramLabel("protocol")
                        .build(),
        };
    }

    private static String checkIfDependantsAreEnabled(String value, ConfigSourceInterceptorContext context) {
        if (isOpenTelemetryLogsEnabled() || isOpenTelemetryTracesEnabled()) {
            return Boolean.TRUE.toString();
        }
        return Boolean.FALSE.toString();
    }

    private static boolean isFeatureEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY);
    }

    public static boolean isOpenTelemetryEnabled() {
        return Configuration.isTrue("quarkus.otel.enabled");
    }

    public static boolean isOpenTelemetryTracesEnabled() {
        return Configuration.isTrue("quarkus.otel.traces.enabled");
    }

    public static boolean isOpenTelemetryLogsEnabled() {
        return Configuration.isTrue(OTEL_LOGS_ENABLED);
    }

    static void validateEndpoint(String value) {
        if (StringUtil.isBlank(value)) {
            throw new PropertyException("Specified Endpoint URL must not be empty.");
        }

        if (!isValidUrl(value)) {
            throw new PropertyException("Specified Endpoint URL is invalid.");
        }
    }

    static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}
