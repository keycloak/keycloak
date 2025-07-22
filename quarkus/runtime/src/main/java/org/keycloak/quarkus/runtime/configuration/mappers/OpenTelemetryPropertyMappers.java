package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.common.Profile;
import org.keycloak.config.LoggingOptions;
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
import static org.keycloak.config.OpenTelemetryOptions.OTEL_PROTOCOL;
import static org.keycloak.config.OpenTelemetryOptions.OTEL_SERVICE_NAME;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class OpenTelemetryPropertyMappers {
    private static final String OTEL_FEATURE_ENABLED_MSG = "'opentelemetry' feature is enabled";
    private static final String OTEL_COLLECTOR_ENABLED_MSG = "something is using OTel collector (Logs, Metrics, Traces)";

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
                        .build(),
                fromOption(OTEL_SERVICE_NAME)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.service.name")
                        .paramLabel("name")
                        .build(),
                fromOption(OTEL_PROTOCOL)
                        .isEnabled(OpenTelemetryPropertyMappers::isOpenTelemetryEnabled, OTEL_COLLECTOR_ENABLED_MSG)
                        .to("quarkus.otel.exporter.otlp.protocol")
                        .paramLabel("protocol")
                        .build(),
                fromOption(LoggingOptions.LOG_OTEL_ENABLED)
                        .to("quarkus.otel.logs.enabled")
                        .build(),
        };
    }

    private static String checkIfDependantsAreEnabled(String value, ConfigSourceInterceptorContext context) {
        if (Configuration.isTrue(TracingOptions.TRACING_ENABLED) || Configuration.isTrue(LoggingOptions.LOG_OTEL_ENABLED)) {
            return Boolean.TRUE.toString();
        }

        return Boolean.FALSE.toString();
    }

    private static boolean isFeatureEnabled() {
        return Profile.isFeatureEnabled(Profile.Feature.OPENTELEMETRY);
    }

    public static boolean isOpenTelemetryEnabled() {
        return Configuration.isTrue(OTEL_ENABLED);
    }

    static void validateEndpoint(String value) {
        if (StringUtil.isBlank(value)) {
            throw new PropertyException("URL specified in 'tracing-endpoint' option must not be empty.");
        }

        if (!isValidUrl(value)) {
            throw new PropertyException("URL specified in 'tracing-endpoint' option is invalid.");
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
