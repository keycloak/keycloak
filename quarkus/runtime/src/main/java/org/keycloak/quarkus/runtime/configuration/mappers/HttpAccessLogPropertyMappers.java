package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.HttpAccessLogOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class HttpAccessLogPropertyMappers implements PropertyMapperGrouping {
    private static final String ACCESS_LOG_ENABLED_MSG = "HTTP Access log is enabled";
    private static final String ACCESS_LOG_FILE_ENABLED_MSG = "HTTP Access logging to file is enabled";

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
                // file
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_FILE_ENABLED)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogEnabled, ACCESS_LOG_ENABLED_MSG)
                        .to("quarkus.http.access-log.log-to-file")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_FILE_NAME)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogFileEnabled, ACCESS_LOG_FILE_ENABLED_MSG)
                        .paramLabel("<name>")
                        .to("quarkus.http.access-log.base-file-name")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_FILE_SUFFIX)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogFileEnabled, ACCESS_LOG_FILE_ENABLED_MSG)
                        .paramLabel("<suffix>")
                        .to("quarkus.http.access-log.log-suffix")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_FILE_ROTATE)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogFileEnabled, ACCESS_LOG_FILE_ENABLED_MSG)
                        .to("quarkus.http.access-log.rotate")
                        .build()
        );
    }

    static boolean isHttpAccessLogEnabled() {
        return Configuration.isTrue(HttpAccessLogOptions.HTTP_ACCESS_LOG_ENABLED);
    }

    static boolean isHttpAccessLogFileEnabled() {
        return Configuration.isTrue(HttpAccessLogOptions.HTTP_ACCESS_LOG_FILE_ENABLED);
    }
}
