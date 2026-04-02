package org.keycloak.quarkus.runtime.configuration;

import org.apache.commons.lang3.StringUtils;

import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.database.Database;
import org.keycloak.quarkus.runtime.configuration.mappers.DatabasePropertyMappers;

import java.util.List;

import static org.keycloak.config.DatabaseOptions.DB;

public class SocketTimeoutInterceptor {
    protected static final String SOCKET_TIMEOUT = "socketTimeout=";

    protected static final boolean isSupported;

    protected static final String dbUrlSocketTimeout;
    protected static final String dbUrlPropertiesSocketTimeout;
    protected static final String dbConnectTimeout;

    private static final List<Database.Vendor> supportedDbVendors =
            List.of(
                    Database.Vendor.MYSQL,
                    Database.Vendor.TIDB
            );

    protected static boolean isInitialized = false;

    static {
        try {
            final String defaultValue = "";

            isSupported = isSupported(
                    Configuration.getConfigValue(DB).getValue());
            dbUrlSocketTimeout = getSocketTimeoutValue(
                    Configuration
                            .getKcConfigValue(DatabaseOptions.DB_URL.getKey())
                            .getValueOrDefault(defaultValue));
            dbUrlPropertiesSocketTimeout = getSocketTimeoutValue(
                    Configuration
                            .getKcConfigValue(DatabaseOptions.DB_URL_PROPERTIES.getKey())
                            .getValueOrDefault(defaultValue));
            dbConnectTimeout =
                    Configuration
                            .getConfigValue(DatabasePropertyMappers.JDBC_LOGIN_TIMEOUT)
                            .getValue();
        } finally {
            isInitialized = true;
        }
    }

    private static boolean isSupported(String dbVendor) {
        return supportedDbVendors.stream()
                .anyMatch((Database.Vendor sp) -> sp.isOfKind(dbVendor));
    }

    public static String getSocketTimeoutValue(String jdbcUrl) {
        String value = StringUtils.substringBetween(jdbcUrl, SOCKET_TIMEOUT, "&");

        if (StringUtils.isBlank(value)) {
            return StringUtils.substringBefore(
                    StringUtils.substringAfter(jdbcUrl, SOCKET_TIMEOUT), "&");
        }

        return value;
    }

}
