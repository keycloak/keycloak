package org.keycloak.quarkus.runtime.configuration;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import io.smallrye.config.Priorities;

import jakarta.annotation.Priority;

import org.apache.commons.lang3.StringUtils;

import org.keycloak.common.util.DurationConverter;

@Priority(Priorities.PLATFORM - 10)
public class JdbcUrlInterceptor extends SocketTimeoutInterceptor implements ConfigSourceInterceptor {

    public static final String QUARKUS_JDBC_URL = "quarkus.datasource.jdbc.url";
    public static final String KC_BC_URL = "kc.db-url";

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {

        if (isAllowed(name)) {
            String connectTimeout = String.valueOf(
                            DurationConverter.parseDuration(dbConnectTimeout).toMillis());

            ConfigValue originalValue =
                    NestedPropertyMappingInterceptor.getValueFromPropertyMappers(context, name);

            if (originalValue != null && originalValue.getValue() != null) {
                String modifiedUrl =
                        modifyDatabaseUrl(originalValue.getValue(), connectTimeout);

                return ConfigValue.builder()
                        .withName(originalValue.getName())
                        .withValue(modifiedUrl)
                        .withRawValue(modifiedUrl)
                        .withProfile(originalValue.getProfile())
                        .withConfigSourceName(originalValue.getConfigSourceName())
                        .withConfigSourceOrdinal(originalValue.getConfigSourceOrdinal())
                        .withConfigSourcePosition(originalValue.getConfigSourcePosition())
                        .withLineNumber(originalValue.getLineNumber())
                        .withProblems(originalValue.getProblems())
                        .build();
            }
        }

        return context.proceed(name);
    }

    private String modifyDatabaseUrl(String originalUrl, String connectTimeout) {
        String socketTimeout = SOCKET_TIMEOUT + connectTimeout;

        if (originalUrl.contains("?")) {
            if (StringUtils.isBlank(getSocketTimeoutValue(originalUrl))) {
                return originalUrl + "&" + socketTimeout;
            }

            String regex = SOCKET_TIMEOUT + "[^&]*";
            return originalUrl.replaceAll(regex, socketTimeout);
        }

        return originalUrl + "?" + socketTimeout;
    }

    private boolean isAllowed(String propertyName) {
        boolean stateCondition = isSupported && isInitialized;
        boolean propertyCondition =
                propertyName.equals(QUARKUS_JDBC_URL) || propertyName.equals(KC_BC_URL);

        return stateCondition && propertyCondition;
    }
}
