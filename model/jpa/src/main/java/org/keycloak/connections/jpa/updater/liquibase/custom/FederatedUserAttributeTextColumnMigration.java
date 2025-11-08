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

import org.keycloak.storage.jpa.JpaHashUtils;

import liquibase.database.core.MySQLDatabase;
import liquibase.exception.CustomChangeException;
import liquibase.statement.core.RawParameterizedSqlStatement;

/**
 * The MySQL database is the only database where columns longer than 255 characters are changed to a TEXT column, allowing
 * for up to 64k characters. See {@link org.keycloak.connections.jpa.updater.liquibase.MySQL8VarcharType} for the implementation.
 * As the new code expects all information longer than 2024 characters in the new column, this migration copies over the values.
 *
 * @author Alexander Schwartz
 */
public class FederatedUserAttributeTextColumnMigration extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {

        if (database instanceof MySQLDatabase) {

            try (PreparedStatement ps = connection.prepareStatement("SELECT t.ID, t.VALUE" +
                    "  FROM " + getTableName("FED_USER_ATTRIBUTE") + " t" +
                    "  WHERE LENGTH(t.VALUE) > 2024");
                 ResultSet resultSet = ps.executeQuery()
            ) {
                while (resultSet.next()) {
                    String id = resultSet.getString(1);
                    String value = resultSet.getString(2);
                    // The SQL LENGTH() will count bytes, where Java's length() will count Unicode characters.
                    // There's also SQL CHAR_LENGTH() which is probably equivalent to Java's Character.codePointCount(),
                    // but it is not a fit here as we're not using code points in the JPA entities
                    if (value.length() > 2024) {
                        statements.add(new RawParameterizedSqlStatement("UPDATE " + getTableName("FED_USER_ATTRIBUTE") + " SET VALUE = null, LONG_VALUE_HASH = ?, LONG_VALUE_HASH_LOWER_CASE = ?, LONG_VALUE = ? WHERE ID = ?",
                                JpaHashUtils.hashForAttributeValue(value),
                                JpaHashUtils.hashForAttributeValueLowerCase(value),
                                value,
                                id));
                    }
                }
            } catch (Exception e) {
                throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
            }
        }

    }

    @Override
    protected String getTaskId() {
        return "Leave only single offline session per user and client";
    }

}
