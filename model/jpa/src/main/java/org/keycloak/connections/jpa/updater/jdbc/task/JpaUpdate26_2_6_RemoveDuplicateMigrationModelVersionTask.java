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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_2_6_RemoveDuplicateMigrationModelVersion}.
 * Removes duplicate VERSION rows from MIGRATION_MODEL, keeping the one with the highest UPDATE_TIME
 * (or highest ID when UPDATE_TIME is equal).
 */
public class JpaUpdate26_2_6_RemoveDuplicateMigrationModelVersionTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Delete duplicated records for DB version in MIGRATION_MODEL table";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        String tableName = NativeSqlHelper.qualified("MIGRATION_MODEL", defaultSchema);
        Set<String> idsToDelete = new HashSet<>();
        List<String> stmts = new ArrayList<>();

        // Find the older of each duplicate pair: same VERSION but lower UPDATE_TIME,
        // or lower ID when UPDATE_TIMEs are equal.
        String query = "SELECT m1.ID FROM " + tableName + " m1"
                + " WHERE EXISTS (SELECT m2.ID FROM " + tableName + " m2"
                + " WHERE m2.VERSION = m1.VERSION"
                + " AND (m2.UPDATE_TIME > m1.UPDATE_TIME"
                + " OR (m2.UPDATE_TIME = m1.UPDATE_TIME AND m2.ID > m1.ID)))";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                idsToDelete.add(rs.getString(1));
            }
        }

        if (idsToDelete.isEmpty()) {
            return stmts;
        }

        // Delete in chunks of 20
        List<String> idList = new ArrayList<>(idsToDelete);
        for (int i = 0; i < idList.size(); i += 20) {
            List<String> chunk = idList.subList(i, Math.min(i + 20, idList.size()));
            String inList = chunk.stream()
                    .map(NativeSqlHelper::sqlLiteral)
                    .collect(java.util.stream.Collectors.joining(", "));
            stmts.add("DELETE FROM " + tableName + " WHERE ID IN (" + inList + ")");
        }
        return stmts;
    }
}
