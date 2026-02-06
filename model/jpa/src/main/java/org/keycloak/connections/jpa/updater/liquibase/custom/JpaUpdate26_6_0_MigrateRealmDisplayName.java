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

import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.CustomChangeException;
import liquibase.statement.core.RawParameterizedSqlStatement;

/**
 * Custom SQL change to migrate the displayName from the REALM_ATTRIBUTE table to the REALM table.
 * See: <a href="https://github.com/keycloak/keycloak/issues/45292">keycloak#45292</a>
 *
 * @author tre2man
 */
public class JpaUpdate26_6_0_MigrateRealmDisplayName extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String realmTable = getTableName("REALM");
        String realmAttributeTable = getTableName("REALM_ATTRIBUTE");

        if (database instanceof PostgresDatabase) {
            generateUpdateQueryForPostgresSQL(realmTable, realmAttributeTable);
        } else if (database instanceof MySQLDatabase) {
            generateUpdateQueryForMySQL(realmTable, realmAttributeTable);
        } else if (database instanceof MSSQLDatabase) {
            generateUpdateQueryForMsSQL(realmTable, realmAttributeTable);
        } else if (database instanceof OracleDatabase) {
            generateUpdateQueryForOracleSQL(realmTable, realmAttributeTable);
        } else {
            generateUpdateQueryUsingStandardSQL(realmTable, realmAttributeTable);
        }

        generateDelete(realmAttributeTable);
    }

    private void generateUpdateQueryForPostgresSQL(String realmTable, String realmAttributeTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE %s r
                SET display_name = ra.value
                FROM %s ra
                WHERE ra.realm_id = r.id and ra.name = 'displayName'
                """
                .formatted(realmTable, realmAttributeTable)));
    }

    private void generateUpdateQueryForMySQL(String realmTable, String realmAttributeTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE %s r
                JOIN %s ra ON r.id = ra.realm_id
                SET r.display_name = ra.value
                WHERE ra.name = 'displayName'
                """
                .formatted(realmTable, realmAttributeTable)));
    }

    private void generateUpdateQueryForMsSQL(String realmTable, String realmAttributeTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE r
                SET display_name = ra.value
                FROM %s r
                JOIN %s ra ON r.id = ra.realm_id
                WHERE ra.name = 'displayName'
                """
                .formatted(realmTable, realmAttributeTable)));
    }

    private void generateUpdateQueryForOracleSQL(String realmTable, String realmAttributeTable) {
        statements.add(new RawParameterizedSqlStatement("""
                MERGE INTO %s r
                USING (
                    SELECT realm_id, value
                    FROM %s
                    WHERE name = 'displayName'
                ) ra
                ON (ra.realm_id = r.id)
                WHEN MATCHED THEN
                    UPDATE SET r.display_name = ra.value
                """
                .formatted(realmTable, realmAttributeTable)));
    }

    private void generateUpdateQueryUsingStandardSQL(String realmTable, String realmAttributeTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE %s r
                SET r.display_name = (
                    SELECT ra.value
                    FROM %s ra
                    WHERE ra.realm_id = r.id AND ra.name = 'displayName'
                )
                WHERE EXISTS (
                    SELECT 1
                    FROM %s ra
                    WHERE ra.realm_id = r.id AND ra.name = 'displayName'
                )
                """
                .formatted(realmTable, realmAttributeTable, realmAttributeTable)));
    }

    private void generateDelete(String realmAttributeTable) {
        statements.add(new RawParameterizedSqlStatement(
                "DELETE FROM %s WHERE name = 'displayName'".formatted(realmAttributeTable)));
    }

    @Override
    protected String getTaskId() {
        return "Migrate displayName from REALM_ATTRIBUTE to REALM.DISPLAY_NAME column";
    }
}
