/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate22_0_5_UpdateAccountTheme}.
 */
public class JpaUpdate22_0_5_UpdateAccountThemeTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update account theme for keycloak 22.0.5";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String realmTable = SqlDialect.qualified("REALM", defaultSchema);
        return List.of(
                "UPDATE " + realmTable
                        + " SET ACCOUNT_THEME = 'keycloak.v2'"
                        + " WHERE ACCOUNT_THEME = 'keycloak'"
        );
    }
}
