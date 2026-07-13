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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class SqlEventHookTargetProviderFactory implements EventHookTargetProviderFactory {

    public static final String ID = "sql";

    private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
            .property()
            .name("databaseKind")
            .label("eventHookTargetDatabaseKind")
            .helpText("eventHookTargetDatabaseKindHelp")
            .type(ProviderConfigProperty.LIST_TYPE)
            .options("h2", "postgres", "mariadb", "mysql", "mssql", "oracle")
            .defaultValue("postgres")
            .required(true)
            .add()
            .property()
            .name("jdbcUrl")
            .label("eventHookTargetJdbcUrl")
            .helpText("eventHookTargetJdbcUrlHelp")
            .type(ProviderConfigProperty.STRING_TYPE)
            .required(true)
            .secret(true)
            .add()
            .property()
            .name("jdbcUsername")
            .label("eventHookTargetJdbcUsername")
            .helpText("eventHookTargetJdbcUsernameHelp")
            .type(ProviderConfigProperty.STRING_TYPE)
            .required(true)
            .secret(true)
            .add()
            .property()
            .name("jdbcPassword")
            .label("eventHookTargetJdbcPassword")
            .helpText("eventHookTargetJdbcPasswordHelp")
            .type(ProviderConfigProperty.PASSWORD)
            .secret(true)
            .add()
            .property()
            .name("sqlStatement")
            .label("eventHookTargetSqlStatement")
            .helpText("eventHookTargetSqlStatementHelp")
            .type(ProviderConfigProperty.TEXT_TYPE)
            .required(true)
            .add()
            .property()
            .name("queryTimeoutSeconds")
            .label("eventHookTargetSqlQueryTimeoutSeconds")
            .helpText("eventHookTargetSqlQueryTimeoutSecondsHelp")
            .type(ProviderConfigProperty.INTEGER_TYPE)
            .defaultValue(30)
            .add()
            .build();

    @Override
    public EventHookTargetProvider create(KeycloakSession session) {
        return new SqlEventHookTargetProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return CONFIG;
    }

    @Override
    public String getDisplayInfo(EventHookTargetModel target) {
        String databaseKind = stringValue(target.getSettings(), "databaseKind", false);
        return databaseKind == null ? "SQL" : databaseKind.toUpperCase(Locale.ROOT) + ": prepared statement";
    }

    @Override
    public void validateConfig(KeycloakSession session, Map<String, Object> settings) {
        String databaseKind = stringValue(settings, "databaseKind", true);
        SqlEventHookTargetProvider.DatabaseType databaseType = SqlEventHookTargetProvider.DatabaseType.from(databaseKind);
        requireDriver(databaseType);

        String jdbcUrl = stringValue(settings, "jdbcUrl", true);
        if (!jdbcUrl.startsWith("jdbc:")) {
            throw new IllegalArgumentException("Target JDBC URL must start with jdbc:");
        }

        stringValue(settings, "jdbcUsername", true);
        String sqlStatement = stringValue(settings, "sqlStatement", true);
        SqlPreparedStatementTemplate preparedStatement = SqlPreparedStatementTemplate.from(sqlStatement, settings.get("sqlParameters"));
        if (placeholderCount(preparedStatement.statement()) != preparedStatement.parameterMappings().size()) {
            throw new IllegalArgumentException("Prepared statement placeholder count must match configured SQL parameters");
        }
        positiveInteger(settings, "queryTimeoutSeconds");
    }

    private void requireDriver(SqlEventHookTargetProvider.DatabaseType databaseType) {
        try {
            Class.forName(databaseType.driverClassName());
        } catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException("JDBC driver not available for database type: " + databaseType.id(), exception);
        }
    }

    private int placeholderCount(String sqlStatement) {
        int count = 0;
        for (int index = 0; index < sqlStatement.length(); index++) {
            if (sqlStatement.charAt(index) == '?') {
                count++;
            }
        }
        return count;
    }

    private String stringValue(Map<String, Object> settings, String key, boolean required) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("Missing required setting: " + key);
            }
            return null;
        }

        String stringValue = value.toString().trim();
        if (required && stringValue.isEmpty()) {
            throw new IllegalArgumentException("Missing required setting: " + key);
        }
        return stringValue.isEmpty() ? null : stringValue;
    }

    private void positiveInteger(Map<String, Object> settings, String key) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return;
        }

        int numericValue = value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
        if (numericValue <= 0) {
            throw new IllegalArgumentException("Setting must be greater than zero: " + key);
        }
    }
}
