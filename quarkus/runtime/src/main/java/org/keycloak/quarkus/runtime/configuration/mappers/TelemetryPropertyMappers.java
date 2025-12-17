package org.keycloak.quarkus.runtime.configuration.mappers;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.TelemetryOptions;
import org.keycloak.config.TracingOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.config.TelemetryOptions.TELEMETRY_ENABLED;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_ENDPOINT;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_LOGS_ENABLED;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_LOGS_ENDPOINT;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_LOGS_LEVEL;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_LOGS_PROTOCOL;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_METRICS_ENABLED;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_METRICS_ENDPOINT;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_METRICS_INTERVAL;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_METRICS_PROTOCOL;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_PROTOCOL;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_RESOURCE_ATTRIBUTES;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_SERVICE_NAME;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class TelemetryPropertyMappers implements PropertyMapperGrouping{
    private static final String OTEL_FEATURE_ENABLED_MSG = "'opentelemetry' feature is enabled";
    private static final String OTEL_COLLECTOR_ENABLED_MSG = "any of available OpenTelemetry components (Logs, Metrics, Traces) is turned on";
    private static final String OTEL_LOGS_FEATURE_ENABLED_MSG = "feature '%s' is enabled".formatted(Profile.Feature.OPENTELEMETRY_LOGS.getVersionedKey());
    private static final String OTEL_LOGS_ENABLED_MSG = "Telemetry Logs functionality ('%s') is enabled".formatted(TELEMETRY_LOGS_ENABLED.getKey());
    private static final String OTEL_METRICS_FEATURE_ENABLED_MSG = "metrics and feature '%s' are enabled".formatted(Profile.Feature.OPENTELEMETRY_METRICS.getVersionedKey());
    private static final String OTEL_METRICS_ENABLED_MSG = "metrics ('%s') and Telemetry Metrics functionality ('%s') are enabled".formatted(MetricsOptions.METRICS_ENABLED.getKey(), TELEMETRY_METRICS_ENABLED.getKey());

    @Override
    public List<? extends PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(TELEMETRY_ENABLED)
                        .isEnabled(TelemetryPropertyMappers::isOtelFeatureEnabled, OTEL_FEATURE_ENABLED_MSG)
                        .transformer(TelemetryPropertyMappers::checkIfDependantsAreEnabled)
                        .to("quarkus.otel.enabled")
                        .build(),
                fromOption(TELEMETRY_ENDPOINT)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.exporter.otlp.endpoint")
                        .paramLabel("url")
                        .validator(TelemetryPropertyMappers::validateEndpoint)
                        .build(),
                fromOption(TELEMETRY_SERVICE_NAME)
                        .mapFrom(TracingOptions.TRACING_SERVICE_NAME) // the tracing option is deprecated, but we need to be backward compatible
                        .isEnabled(TelemetryPropertyMappers::isTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.service.name")
                        .paramLabel("name")
                        .build(),
                fromOption(TELEMETRY_PROTOCOL)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.exporter.otlp.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(TELEMETRY_RESOURCE_ATTRIBUTES)
                        .mapFrom(TracingOptions.TRACING_RESOURCE_ATTRIBUTES) // the tracing option is deprecated, but we need to be backward compatible
                        .isEnabled(TelemetryPropertyMappers::isTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.resource.attributes")
                        .paramLabel("attributes")
                        .build(),
                // Telemetry Logs
                fromOption(TELEMETRY_LOGS_ENABLED)
                        .isEnabled(TelemetryPropertyMappers::isOtelLogsFeatureEnabled, OTEL_LOGS_FEATURE_ENABLED_MSG)
                        .to("quarkus.otel.logs.enabled")
                        .build(),
                fromOption(TELEMETRY_LOGS_ENDPOINT)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryLogsEnabled, OTEL_LOGS_ENABLED_MSG)
                        .mapFrom(TelemetryOptions.TELEMETRY_ENDPOINT)
                        .to("quarkus.otel.exporter.otlp.logs.endpoint")
                        .validator(TelemetryPropertyMappers::validateEndpoint)
                        .paramLabel("url")
                        .build(),
                fromOption(TELEMETRY_LOGS_PROTOCOL)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryLogsEnabled, OTEL_LOGS_ENABLED_MSG)
                        .mapFrom(TelemetryOptions.TELEMETRY_PROTOCOL)
                        .to("quarkus.otel.exporter.otlp.logs.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(TELEMETRY_LOGS_LEVEL)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryLogsEnabled, OTEL_LOGS_ENABLED_MSG)
                        .to("quarkus.otel.logs.level")
                        .paramLabel("level")
                        .transformer(LoggingPropertyMappers::upperCase)
                        .build(),
                // Telemetry Metrics
                fromOption(TELEMETRY_METRICS_ENABLED)
                        .isEnabled(TelemetryPropertyMappers::isOtelMetricsFeatureEnabled, OTEL_METRICS_FEATURE_ENABLED_MSG)
                        .to("quarkus.otel.metrics.enabled")
                        .build(),
                fromOption(TELEMETRY_METRICS_ENDPOINT)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryMetricsEnabled, OTEL_METRICS_ENABLED_MSG)
                        .mapFrom(TelemetryOptions.TELEMETRY_ENDPOINT)
                        .to("quarkus.otel.exporter.otlp.metrics.endpoint")
                        .paramLabel("url")
                        .validator(TelemetryPropertyMappers::validateEndpoint)
                        .build(),
                fromOption(TELEMETRY_METRICS_PROTOCOL)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryMetricsEnabled, OTEL_METRICS_ENABLED_MSG)
                        .mapFrom(TelemetryOptions.TELEMETRY_PROTOCOL)
                        .to("quarkus.otel.exporter.otlp.metrics.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(TELEMETRY_METRICS_INTERVAL)
                        .isEnabled(TelemetryPropertyMappers::isTelemetryMetricsEnabled, OTEL_METRICS_ENABLED_MSG)
                        .to("quarkus.otel.metric.export.interval")
                        .paramLabel("duration")
                        .validator(TelemetryPropertyMappers::validateDuration)
                        .build()
        );
    }

    private static String checkIfDependantsAreEnabled(String value, ConfigSourceInterceptorContext context) {
        if (TelemetryPropertyMappers.isTelemetryLogsEnabled() || TelemetryPropertyMappers.isTelemetryMetricsEnabled() || TracingPropertyMappers.isTracingEnabled()) {
            return Boolean.TRUE.toString();
        }
        return Boolean.FALSE.toString();
    }

    private static boolean isOtelFeatureEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY);
    }

    public static boolean isOtelLogsFeatureEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY_LOGS);
    }

    public static boolean isOtelMetricsFeatureEnabled() {
        return MetricsPropertyMappers.metricsEnabled() && Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY_METRICS);
    }

    public static boolean isTelemetryEnabled() {
        return Configuration.isTrue("quarkus.otel.enabled");
    }

    public static boolean isTelemetryLogsEnabled() {
        return Configuration.isTrue("quarkus.otel.logs.enabled");
    }

    public static boolean isTelemetryMetricsEnabled() {
        return MetricsPropertyMappers.metricsEnabled() && Configuration.isTrue("quarkus.otel.metrics.enabled");
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

    private static void validateDuration(String value) {
        try {
            var duration = DurationConverter.parseDuration(value);
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new PropertyException("Duration specified via '%s' is invalid.".formatted(TELEMETRY_METRICS_INTERVAL.getKey()));
        }
    }
}
