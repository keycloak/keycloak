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
import liquibase.structure.core.Table;

public class JpaUpdate26_8_0_PopulateOrgDomainRealmId extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String orgDomainTable = getTableName(database.correctObjectName("ORG_DOMAIN", Table.class));
        String orgTable = getTableName(database.correctObjectName("ORG", Table.class));

        // A single set-based UPDATE works both in update mode and in script mode:
        // script mode emits this as one SQL statement that is safe to apply at any time
        statements.add(new RawParameterizedSqlStatement(
                "UPDATE " + orgDomainTable + " SET REALM_ID = " +
                "(SELECT o.REALM_ID FROM " + orgTable + " o WHERE o.ID = " + orgDomainTable + ".ORG_ID) " +
                "WHERE " + orgDomainTable + ".REALM_ID IS NULL"
        ));
        confirmationMessage.append("Populated REALM_ID for ORG_DOMAIN rows from ORG.");
    }

    @Override
    protected String getTaskId() {
        return "Populate ORG_DOMAIN.REALM_ID from ORG.REALM_ID";
    }
}
