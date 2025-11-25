package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.HttpAccessLogOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class HttpAccessLogPropertyMappers implements PropertyMapperGrouping {
    public static final String ACCESS_LOG_ENABLED_MSG = "HTTP Access log is enabled";

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_ENABLED)
                        .to("quarkus.http.access-log.enabled")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_PATTERN)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogEnabled, ACCESS_LOG_ENABLED_MSG)
                        .to("quarkus.http.access-log.pattern")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_EXCLUDE)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogEnabled, ACCESS_LOG_ENABLED_MSG)
                        .to("quarkus.http.access-log.exclude-pattern")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_MASKED_COOKIES)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogEnabled, ACCESS_LOG_ENABLED_MSG)
                        .to("quarkus.http.access-log.masked-cookies") // not existing in current Quarkus version yet. Used in the CustomAllRequestHeadersAttribute
                        .build()
        );
    }

    static boolean isHttpAccessLogEnabled() {
        return Configuration.isTrue(HttpAccessLogOptions.HTTP_ACCESS_LOG_ENABLED);
    }
}
