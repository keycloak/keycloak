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

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

/**
 * Custom SQL change to migrate the displayName from the REALM_ATTRIBUTE table to the REALM table.
 * See: <a href="https://github.com/keycloak/keycloak/issues/45356">keycloak#45356</a>
 * @author tre2man
 */
public class JpaUpdate26_6_0_MigrateRealmDisplayName extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        // Move displayName from REALM_ATTRIBUTE to REALM table
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ra.REALM_ID, ra.VALUE " +
                "FROM " + getTableName("REALM_ATTRIBUTE") + " ra " +
                "WHERE ra.NAME = 'displayName'");
             ResultSet resultSet = ps.executeQuery()
        ) {
            while (resultSet.next()) {
                String realmId = resultSet.getString(1);
                String displayNameValue = resultSet.getString(2);

                statements.add(new UpdateStatement(null, null,
                        database.correctObjectName("REALM", Table.class))
                        .addNewColumnValue("DISPLAY_NAME", displayNameValue)
                        .setWhereClause("ID=?")
                        .addWhereParameter(realmId));
            }

            // Delete the displayName entries from REALM_ATTRIBUTE
            statements.add(new DeleteStatement(null, null,
                    database.correctObjectName("REALM_ATTRIBUTE", Table.class))
                    .setWhere("NAME=?")
                    .addWhereParameter("displayName"));

            confirmationMessage.append("Migrated displayName from REALM_ATTRIBUTE to REALM table");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() +
                    ": Exception when migrating displayName data", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Migrate displayName from REALM_ATTRIBUTE to REALM.DISPLAY_NAME column";
    }
}
