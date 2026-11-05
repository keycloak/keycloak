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
import liquibase.statement.core.RawParameterizedSqlStatement;
import liquibase.structure.core.Table;

public class JpaUpdate26_7_0_RealmDisplayName extends CustomKeycloakTask {

    // Matches the REALM.DISPLAY_NAME column added by the change set 26.7.0-45292-realm-display-name-add-column
    // (NVARCHAR(255)). REALM_ATTRIBUTE.VALUE has no length limit and older Keycloak releases accepted display names
    // longer than this column, so values are truncated during the migration as documented in the upgrading guide.
    private static final int DISPLAY_NAME_MAX_LENGTH = 255;

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String realmTable = database.correctObjectName("REALM", Table.class);
        String realmAttributeTable = database.correctObjectName("REALM_ATTRIBUTE", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                     "SELECT REALM_ID, VALUE FROM " + getTableName(realmAttributeTable) + " WHERE NAME = 'displayName'");
             ResultSet rs = ps.executeQuery()) {

            int truncated = 0;
            while (rs.next()) {
                String realmId = rs.getString(1);
                String displayName = rs.getString(2);
                String truncatedDisplayName = truncateDisplayName(displayName);
                if (displayName != null && !displayName.equals(truncatedDisplayName)) {
                    truncated++;
                }
                if (truncatedDisplayName == null) {
                    // The column was added by the change set 26.7.0-45292-realm-display-name-add-column and starts
                    // out NULL, so there is nothing to write for a realm without a display name.
                    continue;
                }
                // The value is bound as a JDBC parameter: an UpdateStatement would serialize a display name equal to
                // "NULL" (case-insensitively) as the SQL NULL keyword and erase it, and the change set
                // 26.7.0-45292-realm-display-name-remove-attribute then deletes the attribute it was copied from.
                statements.add(new RawParameterizedSqlStatement(
                        "UPDATE " + getTableName(realmTable) + " SET DISPLAY_NAME = ? WHERE ID = ?",
                        truncatedDisplayName, realmId));
            }
            confirmationMessage.append("Migrated displayName from REALM_ATTRIBUTE to REALM.DISPLAY_NAME");
            if (truncated > 0) {
                confirmationMessage.append(", " + truncated + " value(s) were truncated to fit the column");
            }
            confirmationMessage.append(". ");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when migrating displayName", e);
        }
    }

    /**
     * Truncates the value to fit the DISPLAY_NAME column on every database supported by Keycloak. The column accepts
     * at most 255 characters, and none of the supported databases accepts more than 255 UTF-16 code units there
     * (SQL Server counts UTF-16 code units while the other databases count code points). Truncating at 255 code units
     * without splitting a surrogate pair therefore fits both counting conventions.
     */
    private String truncateDisplayName(String displayName) {
        if (displayName == null || displayName.length() <= DISPLAY_NAME_MAX_LENGTH) {
            return displayName;
        }
        int endIndex = DISPLAY_NAME_MAX_LENGTH;
        if (Character.isLowSurrogate(displayName.charAt(endIndex))) {
            // The code unit at the limit is the low half of a surrogate pair; drop the whole pair instead of splitting it.
            endIndex--;
        }
        return displayName.substring(0, endIndex);
    }

    @Override
    protected String getTaskId() {
        return "Migrate displayName from REALM_ATTRIBUTE to REALM column";
    }
}
