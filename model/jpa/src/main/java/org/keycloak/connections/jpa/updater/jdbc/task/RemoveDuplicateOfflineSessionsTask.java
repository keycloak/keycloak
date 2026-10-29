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
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code RemoveDuplicateOfflineSessions}.
 * Removes duplicate OFFLINE_CLIENT_SESSION rows, keeping the first occurrence per
 * (USER_SESSION_ID, CLIENT_ID, OFFLINE_FLAG) group.
 */
public class RemoveDuplicateOfflineSessionsTask implements JpaMigrationTask {

    private record Key(String userSessionId, String clientId, String offlineFlag) {}

    @Override
    public String getTaskId() {
        return "Leave only single offline session per user and client";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String tableName = NativeSqlHelper.qualified("OFFLINE_CLIENT_SESSION", defaultSchema);
        Set<String> idsToDelete = new HashSet<>();
        List<String> stmts = new ArrayList<>();

        String query = String.format(
                "SELECT t.CLIENT_SESSION_ID, t.USER_SESSION_ID, t.CLIENT_ID, t.OFFLINE_FLAG"
                + "  FROM %1$s t,"
                + "    (SELECT USER_SESSION_ID, CLIENT_ID, OFFLINE_FLAG"
                + "      FROM %1$s"
                + "    GROUP BY USER_SESSION_ID, CLIENT_ID, OFFLINE_FLAG"
                + "    HAVING COUNT(*) > 1) t1"
                + "  WHERE t.USER_SESSION_ID = t1.USER_SESSION_ID"
                + "    AND t.CLIENT_ID = t1.CLIENT_ID"
                + "    AND t.OFFLINE_FLAG = t1.OFFLINE_FLAG"
                + "  ORDER BY t.USER_SESSION_ID, t.CLIENT_ID, t.OFFLINE_FLAG",
                tableName);

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            Key origKey = new Key(null, null, null);
            while (rs.next()) {
                String clientSessionId = rs.getString(1);
                Key key = new Key(rs.getString(2), rs.getString(3), rs.getString(4));
                if (key.equals(origKey)) {
                    idsToDelete.add(clientSessionId);
                } else {
                    origKey = key;
                }
            }
        }

        // Delete in chunks of 20
        List<String> idList = new ArrayList<>(idsToDelete);
        for (int i = 0; i < idList.size(); i += 20) {
            List<String> chunk = idList.subList(i, Math.min(i + 20, idList.size()));
            String inList = chunk.stream()
                    .map(NativeSqlHelper::sqlLiteral)
                    .collect(java.util.stream.Collectors.joining(", "));
            stmts.add("DELETE FROM " + tableName + " WHERE CLIENT_SESSION_ID IN (" + inList + ")");
        }
        return stmts;
    }
}
