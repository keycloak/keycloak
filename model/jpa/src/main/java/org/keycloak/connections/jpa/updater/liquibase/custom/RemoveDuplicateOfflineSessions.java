/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.structure.core.Column;

/**
 *
 * @author hmlnarik
 */
public class RemoveDuplicateOfflineSessions extends CustomKeycloakTask {

    private static class Key {
        private final String userSessionId;
        private final String clientId;
        private final String offlineFlag;

        public Key(String userSessionId, String clientId, String offlineFlag) {
            this.userSessionId = userSessionId;
            this.clientId = clientId;
            this.offlineFlag = offlineFlag;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.userSessionId);
            hash = 73 * hash + Objects.hashCode(this.clientId);
            hash = 73 * hash + Objects.hashCode(this.offlineFlag);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;

            return Objects.equals(this.userSessionId, other.userSessionId)
              && Objects.equals(this.clientId, other.clientId)
              && Objects.equals(this.offlineFlag, other.offlineFlag);
        }

    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        Set<String> clientSessionIdsToDelete = new HashSet<>();

        String tableName = getTableName("OFFLINE_CLIENT_SESSION");
        String colClientSessionId = database.correctObjectName("CLIENT_SESSION_ID", Column.class);

        try (PreparedStatement ps = connection.prepareStatement(String.format(
            "SELECT t.CLIENT_SESSION_ID, t.USER_SESSION_ID, t.CLIENT_ID, t.OFFLINE_FLAG" +
            "  FROM %1$s t," +
            "    (SELECT USER_SESSION_ID, CLIENT_ID, OFFLINE_FLAG" +
            "      FROM %1$s" +
            "    GROUP BY USER_SESSION_ID, CLIENT_ID, OFFLINE_FLAG" +
            "    HAVING COUNT(*) > 1) t1" +
            "  WHERE t.USER_SESSION_ID = t1.USER_SESSION_ID" +
            "    AND t.CLIENT_ID = t1.CLIENT_ID" +
            "    AND t.OFFLINE_FLAG = t1.OFFLINE_FLAG" +
            "  ORDER BY t.USER_SESSION_ID, t.CLIENT_ID, t.OFFLINE_FLAG", tableName));

            ResultSet resultSet = ps.executeQuery()
          ) {
            // Find out all offending duplicates, keep first row only
            Key origKey = new Key(null, null, null);
            while (resultSet.next()) {
                String clientSessionId = resultSet.getString(1);
                Key key = new Key(resultSet.getString(2), resultSet.getString(3), resultSet.getString(4));

                if (key.equals(origKey)) {
                    clientSessionIdsToDelete.add(clientSessionId);
                } else {
                    origKey = key;
                }
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }

        AtomicInteger ai = new AtomicInteger();
        clientSessionIdsToDelete.stream()
          .collect(Collectors.groupingByConcurrent(id -> ai.getAndIncrement() / 20, Collectors.toList())) // Split into chunks of at most 20 items

          .values().stream()
          .map(ids -> new DeleteStatement(null, null, "OFFLINE_CLIENT_SESSION")
            .setWhere(":name IN (" + ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")")
            .addWhereColumnName(colClientSessionId)
            .addWhereParameters(ids.toArray())
          )
          .forEach(statements::add);
    }

    @Override
    protected String getTaskId() {
        return "Leave only single offline session per user and client";
    }

}
