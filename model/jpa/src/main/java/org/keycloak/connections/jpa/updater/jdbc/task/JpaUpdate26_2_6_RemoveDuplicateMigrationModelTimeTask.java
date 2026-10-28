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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_2_6_RemoveDuplicateMigrationModelTime}.
 * Removes rows from MIGRATION_MODEL that have the same UPDATE_TIME as another row, keeping
 * the highest Keycloak version.
 */
public class JpaUpdate26_2_6_RemoveDuplicateMigrationModelTimeTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Delete duplicated records for DB update time in MIGRATION_MODEL table";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        String tableName = NativeSqlHelper.qualified("MIGRATION_MODEL", defaultSchema);
        Map<String, ModelVersion> itemsToDelete = new HashMap<>();
        List<String> stmts = new ArrayList<>();

        String query = "SELECT m1.ID, m1.VERSION FROM " + tableName + " m1"
                + " WHERE EXISTS (SELECT m2.ID FROM " + tableName + " m2"
                + " WHERE m2.UPDATE_TIME = m1.UPDATE_TIME AND m2.ID <> m1.ID)";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itemsToDelete.put(rs.getString(1), new ModelVersion(rs.getString(2)));
            }
        }

        if (itemsToDelete.isEmpty()) {
            return stmts;
        }

        // Keep the highest version — find the ID to retain
        String highestVersionId = itemsToDelete.entrySet().stream()
                .reduce((e1, e2) -> e1.getValue().lessThan(e2.getValue()) ? e2 : e1)
                .map(Map.Entry::getKey)
                .orElse(null);

        // Delete in chunks of 20
        List<String> idsToDelete = itemsToDelete.keySet().stream()
                .filter(id -> !id.equals(highestVersionId))
                .toList();
        for (int i = 0; i < idsToDelete.size(); i += 20) {
            List<String> chunk = idsToDelete.subList(i, Math.min(i + 20, idsToDelete.size()));
            String inList = chunk.stream()
                    .map(NativeSqlHelper::sqlLiteral)
                    .collect(java.util.stream.Collectors.joining(", "));
            stmts.add("DELETE FROM " + tableName + " WHERE ID IN (" + inList + ")");
        }
        return stmts;
    }
}
