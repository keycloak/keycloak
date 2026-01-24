package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.config.HttpAccessLogOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.utils.StringUtil;

import io.smallrye.config.ConfigSourceInterceptorContext;

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
                        .paramLabel("<pattern>")
                        .to("quarkus.http.access-log.pattern")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_EXCLUDE)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogEnabled, ACCESS_LOG_ENABLED_MSG)
                        .paramLabel("<exclusions>")
                        .to("quarkus.http.access-log.exclude-pattern")
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_MASKED_HEADERS)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogEnabled, ACCESS_LOG_ENABLED_MSG)
                        .paramLabel("<headers>")
                        .to("quarkus.http.access-log.masked-headers")
                        .transformer(HttpAccessLogPropertyMappers::transformMaskedHeaders)
                        .build(),
                fromOption(HttpAccessLogOptions.HTTP_ACCESS_LOG_MASKED_COOKIES)
                        .isEnabled(HttpAccessLogPropertyMappers::isHttpAccessLogEnabled, ACCESS_LOG_ENABLED_MSG)
                        .paramLabel("<cookies>")
                        .to("quarkus.http.access-log.masked-cookies")
                        .transformer(HttpAccessLogPropertyMappers::transformMaskedCookies)
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

    private static String transformMaskedHeaders(String value, ConfigSourceInterceptorContext context) {
        return transformMaskedElements(value, HttpAccessLogOptions.DEFAULT_HIDDEN_HEADERS);
    }

    private static String transformMaskedCookies(String value, ConfigSourceInterceptorContext context) {
        return transformMaskedElements(value, HttpAccessLogOptions.DEFAULT_HIDDEN_COOKIES);
    }

    private static String transformMaskedElements(String value, List<String> defaultMaskedElements) {
        var defaultMasked = new ArrayList<>(defaultMaskedElements);
        if (StringUtil.isNotBlank(value)) {
            Arrays.stream(value.split(","))
                    .filter(f -> !defaultMasked.contains(f))
                    .forEach(defaultMasked::add);
        }
        return String.join(",", defaultMasked);
    }


    static boolean isHttpAccessLogEnabled() {
        return Configuration.isTrue(HttpAccessLogOptions.HTTP_ACCESS_LOG_ENABLED);
    }

    static boolean isHttpAccessLogFileEnabled() {
        return Configuration.isTrue(HttpAccessLogOptions.HTTP_ACCESS_LOG_FILE_ENABLED);
    }
}
