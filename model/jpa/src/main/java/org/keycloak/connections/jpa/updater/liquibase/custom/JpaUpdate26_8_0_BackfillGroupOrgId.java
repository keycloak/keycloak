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

public class JpaUpdate26_8_0_BackfillGroupOrgId extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String groupTable = getTableName("KEYCLOAK_GROUP");
        String orgTable = getTableName("ORG");

        statements.add(new RawParameterizedSqlStatement(
                "UPDATE " + groupTable + " SET ORG_ID = " +
                "(SELECT o.ID FROM " + orgTable + " o WHERE o.GROUP_ID = " + groupTable + ".ID) " +
                "WHERE " + groupTable + ".ID IN (SELECT GROUP_ID FROM " + orgTable + ") " +
                "AND " + groupTable + ".ORG_ID IS NULL"
        ));
    }

    @Override
    protected String getTaskId() {
        return "Backfill KEYCLOAK_GROUP.ORG_ID from ORG.GROUP_ID";
    }
}
