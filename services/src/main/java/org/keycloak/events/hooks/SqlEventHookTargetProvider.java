/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.events.hooks;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.keycloak.util.JsonSerialization;

public class SqlEventHookTargetProvider implements EventHookTargetProvider {

    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 30;
    private static final String FULL_PAYLOAD_PARAMETER = "$payload";
    private static final String FULL_PAYLOAD_PREFIX = FULL_PAYLOAD_PARAMETER + ".";

    @Override
    public EventHookDeliveryResult deliver(EventHookTargetModel target, EventHookMessageModel message) throws IOException {
        long started = System.currentTimeMillis();
        try {
            executeStatement(target, message);

            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setSuccess(true);
            result.setRetryable(false);
            result.setStatusCode("SQL_OK");
            result.setDurationMillis(System.currentTimeMillis() - started);
            return result;
        } catch (IllegalArgumentException exception) {
            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setSuccess(false);
            result.setRetryable(false);
            result.setStatusCode("SQL_CONFIG_ERROR");
            result.setDetails(truncate(exception.getMessage(), 1024));
            result.setDurationMillis(System.currentTimeMillis() - started);
            return result;
        } catch (SQLException exception) {
            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setSuccess(false);
            result.setRetryable(true);
            result.setStatusCode(sqlState(exception));
            result.setDetails(truncate(exception.getMessage(), 1024));
            result.setDurationMillis(System.currentTimeMillis() - started);
            return result;
        }
    }

    @Override
    public EventHookDeliveryResult deliverBatch(EventHookTargetModel target, List<EventHookMessageModel> messages) {
        throw new IllegalArgumentException("SQL target does not support batch delivery");
    }

    @Override
    public void close() {
    }

    private void executeStatement(EventHookTargetModel target, EventHookMessageModel message) throws SQLException, IOException {
        Map<String, Object> settings = target.getSettings();
        DatabaseType databaseType = DatabaseType.from(requiredString(settings, "databaseKind"));
        loadDriver(databaseType);

        String jdbcUrl = requiredString(settings, "jdbcUrl");
        String statement = requiredString(settings, "sqlStatement");
        List<String> parameterMappings = parameterMappings(settings.get("sqlParameters"));
        Map<String, Object> payload = readPayload(message.getPayload());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties(settings));
                PreparedStatement preparedStatement = connection.prepareStatement(statement)) {
            preparedStatement.setQueryTimeout(intSetting(settings, "queryTimeoutSeconds", DEFAULT_QUERY_TIMEOUT_SECONDS));
            bindParameters(preparedStatement, parameterMappings, payload, message.getPayload());
            preparedStatement.executeUpdate();
        }
    }

    private void loadDriver(DatabaseType databaseType) {
        try {
            Class.forName(databaseType.driverClassName());
        } catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException("JDBC driver not available for database type: " + databaseType.id(), exception);
        }
    }

    private Properties connectionProperties(Map<String, Object> settings) {
        Properties properties = new Properties();
        properties.setProperty("user", requiredString(settings, "jdbcUsername"));

        String password = optionalString(settings, "jdbcPassword");
        if (password != null) {
            properties.setProperty("password", password);
        }

        return properties;
    }

    private void bindParameters(PreparedStatement preparedStatement, List<String> parameterMappings, Map<String, Object> payload,
            String rawPayload) throws SQLException, IOException {
        for (int index = 0; index < parameterMappings.size(); index++) {
            Object value = parameterValue(parameterMappings.get(index), payload, rawPayload);
            setParameter(preparedStatement, index + 1, value);
        }
    }

    private Object parameterValue(String mapping, Map<String, Object> payload, String rawPayload) throws IOException {
        if (FULL_PAYLOAD_PARAMETER.equals(mapping)) {
            return rawPayload;
        }

        if (mapping != null && mapping.startsWith(FULL_PAYLOAD_PREFIX)) {
            mapping = mapping.substring(FULL_PAYLOAD_PREFIX.length());
        }

        Object value = extractValue(payload, mapping);
        if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
            return JsonSerialization.writeValueAsString(value);
        }
        return value;
    }

    private void setParameter(PreparedStatement preparedStatement, int parameterIndex, Object value) throws SQLException {
        if (value == null) {
            preparedStatement.setObject(parameterIndex, null);
            return;
        }

        if (value instanceof Boolean booleanValue) {
            preparedStatement.setBoolean(parameterIndex, booleanValue);
            return;
        }

        if (value instanceof Integer integerValue) {
            preparedStatement.setInt(parameterIndex, integerValue);
            return;
        }

        if (value instanceof Long longValue) {
            preparedStatement.setLong(parameterIndex, longValue);
            return;
        }

        if (value instanceof Float floatValue) {
            preparedStatement.setFloat(parameterIndex, floatValue);
            return;
        }

        if (value instanceof Double doubleValue) {
            preparedStatement.setDouble(parameterIndex, doubleValue);
            return;
        }

        preparedStatement.setObject(parameterIndex, value);
    }

    private Object extractValue(Object current, String mapping) {
        if (current == null || mapping == null || mapping.isBlank()) {
            return null;
        }

        Object value = current;
        for (String segment : mapping.split("\\.")) {
            if (value instanceof Map<?, ?> mapValue) {
                value = mapValue.get(segment);
            } else if (value instanceof List<?> listValue) {
                int index;
                try {
                    index = Integer.parseInt(segment);
                } catch (NumberFormatException ignored) {
                    return null;
                }
                value = index >= 0 && index < listValue.size() ? listValue.get(index) : null;
            } else {
                return null;
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readPayload(String payload) throws IOException {
        return JsonSerialization.readValue(payload, Map.class);
    }

    private List<String> parameterMappings(Object configuredValue) {
        if (configuredValue == null) {
            return List.of(FULL_PAYLOAD_PARAMETER);
        }

        if (configuredValue instanceof String stringValue) {
            String trimmed = stringValue.trim();
            return trimmed.isEmpty() ? List.of() : List.of(trimmed);
        }

        if (configuredValue instanceof Collection<?> values) {
            return values.stream().map(value -> value == null ? null : value.toString().trim())
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        }

        String trimmed = configuredValue.toString().trim();
        return trimmed.isEmpty() ? List.of() : List.of(trimmed);
    }

    private String requiredString(Map<String, Object> settings, String key) {
        String value = optionalString(settings, key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required setting: " + key);
        }
        return value;
    }

    private String optionalString(Map<String, Object> settings, String key) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return null;
        }
        String stringValue = value.toString().trim();
        return stringValue.isEmpty() ? null : stringValue;
    }

    private int intSetting(Map<String, Object> settings, String key, int defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private String sqlState(SQLException exception) {
        return exception.getSQLState() == null ? "SQL_ERROR" : exception.getSQLState();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    enum DatabaseType {
        H2("h2", "org.h2.Driver"),
        POSTGRES("postgres", "org.postgresql.Driver"),
        MARIADB("mariadb", "org.mariadb.jdbc.Driver"),
        MYSQL("mysql", "com.mysql.cj.jdbc.Driver"),
        MSSQL("mssql", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
        ORACLE("oracle", "oracle.jdbc.OracleDriver");

        private final String id;
        private final String driverClassName;

        DatabaseType(String id, String driverClassName) {
            this.id = id;
            this.driverClassName = driverClassName;
        }

        static DatabaseType from(String id) {
            return java.util.Arrays.stream(values())
                    .filter(value -> value.id.equals(id.toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported database type: " + id));
        }

        String id() {
            return id;
        }

        String driverClassName() {
            return driverClassName;
        }
    }
}
