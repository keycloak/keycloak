package org.keycloak.quarkus.runtime.configuration.mappers;

import io.quarkus.runtime.configuration.DurationConverter;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.TelemetryOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

final class MetricsPropertyMappers {

    public static final String METRICS_ENABLED_MSG = "metrics are enabled";
    public static final String METRICS_EXPORT_ENABLED_MSG = "metrics ('metrics-enabled') and metrics export ('metrics-export-enabled') are enabled";

    private MetricsPropertyMappers(){}

    public static PropertyMapper<?>[] getMetricsPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(MetricsOptions.METRICS_ENABLED)
                        .to("quarkus.micrometer.enabled")
                        .build(),
                // Export
                fromOption(MetricsOptions.METRICS_EXPORT_ENABLED)
                        .isEnabled(MetricsPropertyMappers::metricsEnabled, MetricsPropertyMappers.METRICS_ENABLED_MSG)
                        .to("quarkus.otel.metrics.enabled")
                        .build(),
                fromOption(MetricsOptions.METRICS_EXPORT_ENDPOINT)
                        .isEnabled(MetricsPropertyMappers::isExportEnabled, METRICS_EXPORT_ENABLED_MSG)
                        .mapFrom(TelemetryOptions.TELEMETRY_ENDPOINT)
                        .to("quarkus.otel.exporter.otlp.metrics.endpoint")
                        .paramLabel("url")
                        .validator(TelemetryPropertyMappers::validateEndpoint)
                        .build(),
                fromOption(MetricsOptions.METRICS_EXPORT_PROTOCOL)
                        .isEnabled(MetricsPropertyMappers::isExportEnabled, METRICS_EXPORT_ENABLED_MSG)
                        .mapFrom(TelemetryOptions.TELEMETRY_PROTOCOL)
                        .to("quarkus.otel.exporter.otlp.metrics.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(MetricsOptions.METRICS_EXPORT_INTERVAL)
                        .isEnabled(MetricsPropertyMappers::isExportEnabled, METRICS_EXPORT_ENABLED_MSG)
                        .to("quarkus.otel.metric.export.interval")
                        .paramLabel("duration")
                        .validator(MetricsPropertyMappers::validateDuration)
                        .build(),
        };
    }

    public static boolean metricsEnabled() {
        return isTrue(MetricsOptions.METRICS_ENABLED);
    }

    public static boolean isExportEnabled() {
        return metricsEnabled() && isTrue(MetricsOptions.METRICS_EXPORT_ENABLED);
    }

    private static void validateDuration(String value) {
        try {
            var duration = DurationConverter.parseDuration(value);
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new PropertyException("Duration specified via '%s' is invalid.".formatted(MetricsOptions.METRICS_EXPORT_INTERVAL.getKey()));
        }
    }
}
