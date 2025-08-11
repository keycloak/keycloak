package org.keycloak.quarkus.runtime.configuration.mappers;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.config.TracingOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.config.TelemetryOptions.TELEMETRY_ENABLED;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_ENDPOINT;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_PROTOCOL;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_RESOURCE_ATTRIBUTES;
import static org.keycloak.config.TelemetryOptions.TELEMETRY_SERVICE_NAME;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class TelemetryPropertyMappers implements PropertyMapperGrouping{
    private static final String OTEL_FEATURE_ENABLED_MSG = "'opentelemetry' feature is enabled";
    private static final String OTEL_COLLECTOR_ENABLED_MSG = "any of available OpenTelemetry components (Traces) is turned on";

    @Override
    public List<? extends PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(TELEMETRY_ENABLED)
                        .isEnabled(TelemetryPropertyMappers::isFeatureEnabled, OTEL_FEATURE_ENABLED_MSG)
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
                        .build()
        );
    }

    private static String checkIfDependantsAreEnabled(String value, ConfigSourceInterceptorContext context) {
        if (Configuration.isTrue(TracingOptions.TRACING_ENABLED)) {
            return Boolean.TRUE.toString();
        }
        return Boolean.FALSE.toString();
    }

    private static boolean isFeatureEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY);
    }

    public static boolean isTelemetryEnabled() {
        return Configuration.isTrue("quarkus.otel.enabled");
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
