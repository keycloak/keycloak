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
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

public class JpaUpdate26_8_0_PopulateOrgDomainJoinMigration extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String orgDomainOldTable = database.correctObjectName("ORG_DOMAIN_OLD", Table.class);
        String orgTable = database.correctObjectName("ORG", Table.class);
        String domainTable = database.correctObjectName("DOMAIN", Table.class);
        String orgDomainTable = database.correctObjectName("ORG_DOMAIN", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT dom.ID, od.ORG_ID FROM " + getTableName(orgDomainOldTable) +
                        " od JOIN " + getTableName(orgTable) + " o ON od.ORG_ID = o.ID" +
                        " JOIN " + getTableName(domainTable) + " dom ON dom.NAME = od.NAME AND dom.REALM_ID = o.REALM_ID");
             ResultSet rs = ps.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                statements.add(new InsertStatement(null, null, orgDomainTable)
                        .addColumnValue("DOMAIN_ID", rs.getString(1))
                        .addColumnValue("ORG_ID", rs.getString(2)));
                count++;
            }
            confirmationMessage.append("Populated ORG_DOMAIN join table with ").append(count).append(" rows.");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when populating ORG_DOMAIN join table", e);
        }
    }

    @Override
    protected String getTaskId() {
        return "Populate ORG_DOMAIN join table from ORG_DOMAIN_OLD data";
    }
}
