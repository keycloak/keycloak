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
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_5_0_OrgTablesSetDefaultCharsetAndCollation}.
 * MySQL/MariaDB only: ensures ORG.ID and ORG_INVITATION table use the database default charset/collation.
 */
public class JpaUpdate26_5_0_OrgTablesSetDefaultCharsetAndCollationTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Update ORG and ORG_INVITATION tables charset and collation";
    }

    /** Must also run during initial database creation, when the REALM table is still empty. */
    @Override
    public boolean isApplicable(Connection connection, String defaultSchema) {
        return true;
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema)
            throws SQLException {
        SqlDialect dialect = SqlDialect.of(connection);
        if (dialect != SqlDialect.MYSQL && dialect != SqlDialect.MARIADB) {
            return List.of();
        }

        // Check ORG table exists
        if (!tableExists(connection, defaultSchema, "ORG")) {
            return List.of();
        }

        String orgTable = NativeSqlHelper.qualified("ORG", defaultSchema);
        String orgInvTable = NativeSqlHelper.qualified("ORG_INVITATION", defaultSchema);
        List<String> stmts = new ArrayList<>();

        // Read current schema charset and collation
        String schemaName = defaultSchema;
        if (schemaName == null || schemaName.isBlank()) {
            // Use the connection's current database
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
                if (rs.next()) {
                    schemaName = rs.getString(1);
                }
            }
        }
        if (schemaName == null || schemaName.isBlank()) {
            return List.of();
        }

        String charset;
        String collation;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME"
                             + " FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '"
                             + schemaName.replace("'", "''") + "'")) {
            if (!rs.next()) {
                return List.of();
            }
            charset = rs.getString(1);
            collation = rs.getString(2);
        }

        boolean hasOrgInv = tableExists(connection, defaultSchema, "ORG_INVITATION");

        stmts.add("SET FOREIGN_KEY_CHECKS=0");

        if (hasOrgInv) {
            stmts.add("ALTER TABLE " + orgInvTable + " DROP FOREIGN KEY FK_ORG_INVITATION_ORG");
            stmts.add("ALTER TABLE " + orgInvTable
                    + " CONVERT TO CHARACTER SET " + charset + " COLLATE " + collation);
        }

        stmts.add("ALTER TABLE " + orgTable
                + " MODIFY ID VARCHAR(255) CHARACTER SET " + charset + " COLLATE " + collation + " NOT NULL");

        if (hasOrgInv) {
            stmts.add("ALTER TABLE " + orgInvTable
                    + " ADD CONSTRAINT FK_ORG_INVITATION_ORG FOREIGN KEY (ORGANIZATION_ID) REFERENCES "
                    + orgTable + " (ID)");
        }

        stmts.add("SET FOREIGN_KEY_CHECKS=1");
        return stmts;
    }

    private static boolean tableExists(Connection conn, String schema, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, schema, tableName, null)) {
            if (rs.next()) return true;
        }
        // Also try uppercase
        try (ResultSet rs = conn.getMetaData().getTables(null, schema,
                tableName.toUpperCase(java.util.Locale.ROOT), null)) {
            return rs.next();
        }
    }
}
