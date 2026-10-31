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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate1_4_0_Final}.
 * Generates UUIDs for existing USER_ATTRIBUTE rows.
 */
public class JpaUpdate1_4_0_FinalTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 1.4.0.Final";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String table = SqlDialect.qualified("USER_ATTRIBUTE", defaultSchema);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT NAME, USER_ID FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                String userId = rs.getString(2);
                // Use literal values in WHERE clause
                stmts.add("UPDATE " + table
                        + " SET ID = " + dialect.literal(KeycloakModelUtils.generateId())
                        + " WHERE NAME = " + dialect.literal(name)
                        + " AND USER_ID = " + dialect.literal(userId));
            }
        }
        return stmts;
    }
}
