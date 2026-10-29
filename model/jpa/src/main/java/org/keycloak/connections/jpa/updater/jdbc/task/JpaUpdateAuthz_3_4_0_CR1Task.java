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
import java.util.ArrayList;
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdateAuthz_3_4_0_CR1}.
 * Back-fills RESOURCE_SERVER_CLIENT_ID for authorization tables from RESOURCE_SERVER.
 * MSSQL requires the FROM-clause UPDATE syntax; all other dialects use a correlated subquery.
 */
public class JpaUpdateAuthz_3_4_0_CR1Task implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update authz-3.4.0.CR1-resource-server-pk-change-part2";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        if (!NativeSqlHelper.isApplicable(connection, defaultSchema, dialect)) {
            return List.of();
        }
        String rsTable = NativeSqlHelper.qualified("RESOURCE_SERVER", defaultSchema);
        List<String> stmts = new ArrayList<>();
        for (String detail : new String[]{"RESOURCE_SERVER_POLICY", "RESOURCE_SERVER_RESOURCE", "RESOURCE_SERVER_SCOPE"}) {
            String detailTable = NativeSqlHelper.qualified(detail, defaultSchema);
            stmts.add(buildUpdate(dialect, rsTable, detailTable));
        }
        return stmts;
    }

    private static String buildUpdate(SqlDialect dialect, String rsTable, String detailTable) {
        if (dialect == SqlDialect.MSSQL) {
            return "UPDATE " + detailTable
                    + " SET RESOURCE_SERVER_CLIENT_ID = s.CLIENT_ID"
                    + " FROM (SELECT ID, CLIENT_ID FROM " + rsTable + ") s"
                    + " WHERE s.ID = " + detailTable + ".RESOURCE_SERVER_ID";
        }
        return "UPDATE " + detailTable + " p"
                + " SET RESOURCE_SERVER_CLIENT_ID = ("
                + "SELECT CLIENT_ID FROM " + rsTable + " s WHERE s.ID = p.RESOURCE_SERVER_ID)";
    }
}
