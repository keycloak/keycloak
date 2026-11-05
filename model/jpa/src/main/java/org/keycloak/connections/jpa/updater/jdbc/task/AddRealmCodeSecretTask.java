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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Migration task registered for the historic manifest class name {@code AddRealmCodeSecret}.
 * Generates an UPDATE statement for each REALM row where CODE_SECRET is NULL.
 */
public class AddRealmCodeSecretTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Add realm code secret";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String realmTable = SqlDialect.qualified("REALM", defaultSchema);
        List<String> stmts = new ArrayList<>();

        String sql = dialect.firstRowSql(realmTable);
        // Check table exists and has rows
        try (Statement st = connection.createStatement();
             ResultSet check = st.executeQuery(sql)) {
            if (!check.next()) {
                return stmts;
            }
        } catch (SQLException e) {
            return stmts; // table doesn't exist
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT ID FROM " + realmTable + " WHERE CODE_SECRET IS NULL")) {
            while (rs.next()) {
                String id = rs.getString(1);
                stmts.add("UPDATE " + realmTable + " SET CODE_SECRET = "
                        + dialect.literal(KeycloakModelUtils.generateCodeSecret())
                        + " WHERE ID = " + dialect.literal(id));
            }
        }
        return stmts;
    }
}
