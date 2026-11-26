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
import liquibase.statement.core.RawParameterizedSqlStatement;

public class JpaUpdate26_7_0_RealmDisplayName extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String realmTable = getTableName("REALM");
        String realmAttributeTable = getTableName("REALM_ATTRIBUTE");

        statements.add(new RawParameterizedSqlStatement("""
                UPDATE %s
                SET DISPLAY_NAME = (
                    SELECT VALUE FROM %s
                    WHERE REALM_ID = %s.ID AND NAME = 'displayName'
                )
                WHERE EXISTS (
                    SELECT 1 FROM %s
                    WHERE REALM_ID = %s.ID AND NAME = 'displayName'
                )"""
                .formatted(realmTable, realmAttributeTable, realmTable, realmAttributeTable, realmTable)));
    }

    @Override
    protected String getTaskId() {
        return "Migrate displayName from REALM_ATTRIBUTE to REALM column";
    }
}
