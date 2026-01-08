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

import liquibase.exception.CustomChangeException;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.core.Table;

/**
 * Ensures ORG.ID column has correct charset for MySQL/MariaDB to allow foreign keys from ORG_INVITATION table.
 *
 * This fixes the issue where foreign key creation fails due to charset mismatch between ORG.ID and
 * ORG_INVITATION.ORGANIZATION_ID columns on MySQL/MariaDB databases.
 *
 * See https://github.com/keycloak/keycloak/issues/45239
 */
public class JpaUpdate26_5_0_OrgTableCharset extends CustomKeycloakTask {

    @Override
    protected boolean isApplicable() throws CustomChangeException {
        // Always apply this charset fix when ORG table exists
        // Don't check for REALM data as this needs to run during initial database creation
        try {
            // Correct the table name based on the database dialect (quotes, case-sensitivity)
            String correctedTableName = database.correctObjectName("ORG", Table.class);

            // Use the Liquibase Snapshot API to check if the table actually exists in the schema
            return SnapshotGeneratorFactory.getInstance().has(new Table().setName(correctedTableName), database);
        } catch (Exception e) {
            throw new CustomChangeException("Failed to check for existence of ORG table", e);
        }
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String orgTableName = getTableName("ORG");

        // Fix ORG.ID column charset to utf8mb3 to ensure foreign key compatibility with ORG_INVITATION table
        statements.add(new RawSqlStatement(
            "ALTER TABLE " + orgTableName +
            " MODIFY ID VARCHAR(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci NOT NULL"
        ));

        confirmationMessage.append("Updated ORG.ID column charset to utf8mb3 for foreign key compatibility");
    }

    @Override
    protected String getTaskId() {
        return "Update ORG table charset and collation";
    }
}
