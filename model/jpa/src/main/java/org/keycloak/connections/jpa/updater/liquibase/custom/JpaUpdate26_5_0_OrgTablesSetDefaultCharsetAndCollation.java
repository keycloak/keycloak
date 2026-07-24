/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import liquibase.database.core.MySQLDatabase;
import liquibase.exception.CustomChangeException;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;

/**
 * Ensures ORG.ID column and ORG_INVITATION table has set the default charset and collation for MySQL/MariaDB.
 */
public class JpaUpdate26_5_0_OrgTablesSetDefaultCharsetAndCollation extends CustomKeycloakTask {

    @Override
    protected boolean isApplicable() throws CustomChangeException {
        // Only run on MySQL or MariaDB (MariaDBDatabase extends MySQLDatabase)
        if (!(database instanceof MySQLDatabase)) {
            return false;
        }
        // Always apply this charset fix when ORG table exists
        // Don't check for REALM data as this needs to run during initial database creation
        return tableExists("ORG");
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String schemaName = database.escapeObjectName(database.getDefaultSchemaName(), Schema.class);

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT DEFAULT_CHARACTER_SET_NAME as charset, DEFAULT_COLLATION_NAME as collation " +
                     "FROM INFORMATION_SCHEMA.SCHEMATA " +
                     "WHERE SCHEMA_NAME = '" + schemaName + "'"
             )) {

            if (rs.next()) {
                String charset = rs.getString("charset");
                String collation = rs.getString("collation");

                String orgTable = getTableName("ORG");
                String orgInvTable = getTableName("ORG_INVITATION");

                statements.add(new RawSqlStatement("SET FOREIGN_KEY_CHECKS=0"));

                if (tableExists("ORG_INVITATION")) {
                    // Fix ORG_INVITATION.ORGANIZATION_ID charset/collation
                    statements.add(new RawSqlStatement(
                        "ALTER TABLE " + orgInvTable + " DROP FOREIGN KEY FK_ORG_INVITATION_ORG"
                    ));

                    statements.add(new RawSqlStatement(
                        "ALTER TABLE " + orgInvTable +
                        " CONVERT TO CHARACTER SET " + charset + " COLLATE " + collation
                    ));
                }

                // Fix ORG.ID charset/collation
                statements.add(new RawSqlStatement(
                    "ALTER TABLE " + orgTable +
                    " MODIFY ID VARCHAR(255) CHARACTER SET " + charset + " COLLATE " + collation + " NOT NULL"
                ));

                if (tableExists("ORG_INVITATION")) {
                    statements.add(new RawSqlStatement(
                        "ALTER TABLE " + orgInvTable +
                        " ADD CONSTRAINT FK_ORG_INVITATION_ORG FOREIGN KEY (ORGANIZATION_ID) REFERENCES " + orgTable + " (ID)"
                    ));
                }

                statements.add(new RawSqlStatement("SET FOREIGN_KEY_CHECKS=1"));
            }
        } catch (SQLException e) {
            throw new CustomChangeException("Failed to read database default character set", e);
        }

        confirmationMessage.append("Updated charset to default charset and collation for both ORG.ID column and ORG_INVITATION table (if exists)");
    }

    @Override
    protected String getTaskId() {
        return "Update ORG and ORG_INVITATION tables charset and collation";
    }

    private boolean tableExists(String tableName) throws CustomChangeException {
        try {
            // Correct the table name based on the database dialect (quotes, case-sensitivity)
            String correctedTableName = database.correctObjectName(tableName, Table.class);

            // Use the Liquibase Snapshot API to check if the table actually exists in the schema
            return SnapshotGeneratorFactory.getInstance().has(new Table().setName(correctedTableName), database);
        } catch (Exception e) {
            throw new CustomChangeException("Failed to check for existence of ORG table", e);
        }
    }
}
