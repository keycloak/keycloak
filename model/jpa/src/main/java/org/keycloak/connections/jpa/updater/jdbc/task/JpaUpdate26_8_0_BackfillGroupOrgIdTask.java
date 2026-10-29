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
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_8_0_BackfillGroupOrgId}.
 * Sets KEYCLOAK_GROUP.ORG_ID from ORG.GROUP_ID for group rows that represent organizations.
 */
public class JpaUpdate26_8_0_BackfillGroupOrgIdTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Backfill KEYCLOAK_GROUP.ORG_ID from ORG.GROUP_ID";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String groupTable = NativeSqlHelper.qualified("KEYCLOAK_GROUP", defaultSchema);
        String orgTable = NativeSqlHelper.qualified("ORG", defaultSchema);
        return List.of(
                "UPDATE " + groupTable
                        + " SET ORG_ID = (SELECT o.ID FROM " + orgTable + " o WHERE o.GROUP_ID = " + groupTable + ".ID)"
                        + " WHERE " + groupTable + ".ID IN (SELECT GROUP_ID FROM " + orgTable + ")"
                        + " AND " + groupTable + ".ORG_ID IS NULL"
        );
    }
}
