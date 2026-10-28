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

import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate4_7_0_OfflineSessionsTimestamps}.
 * Sets LAST_SESSION_REFRESH on all OFFLINE_USER_SESSION rows that are NULL.
 */
public class JpaUpdate4_7_0_OfflineSessionsTimestampsTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update 4.7.0.Final";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, SqlDialect.of(connection))) {
            return List.of();
        }
        String table = NativeSqlHelper.qualified("OFFLINE_USER_SESSION", defaultSchema);
        int currentTime = Time.currentTime();
        return List.of(
                "UPDATE " + table + " SET LAST_SESSION_REFRESH = " + currentTime
        );
    }
}
