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
import java.util.HashSet;
import java.util.Set;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Table;

/**
 * Records the target organization on every organization group IdP mapper. Until 26.8 the organization was implied by
 * the IdP, which carried exactly one organization; now an IdP can be linked to several, so the mapper has to name the
 * one it applies to.
 * <p>
 * Runs before {@code IDENTITY_PROVIDER.ORGANIZATION_ID} is dropped and reads that column rather than
 * {@code ORG_IDENTITY_PROVIDER} — same data, but it does not depend on the statements of the preceding custom change
 * having been flushed.
 */
public class JpaUpdate26_8_0_IdpMapperOrganizationId extends CustomKeycloakTask {

    // string literals rather than org.keycloak.broker.provider.ConfigConstants, which model/jpa does not depend on
    private static final String GROUP_TYPE = "groupType";
    private static final String ORGANIZATION_ID = "orgId";
    private static final String ORGANIZATION_GROUP_TYPE = "ORGANIZATION";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String mapperConfigTable = database.correctObjectName("IDP_MAPPER_CONFIG", Table.class);
        String mapperTable = database.correctObjectName("IDENTITY_PROVIDER_MAPPER", Table.class);
        String idpTable = database.correctObjectName("IDENTITY_PROVIDER", Table.class);

        try {
            Set<String> alreadyMigrated = getMappersWithOrganizationId(mapperConfigTable);
            int count = 0;

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT c.IDP_MAPPER_ID, c.VALUE, ip.ORGANIZATION_ID FROM " + getTableName(mapperConfigTable) + " c" +
                            " JOIN " + getTableName(mapperTable) + " m ON m.ID = c.IDP_MAPPER_ID" +
                            " JOIN " + getTableName(idpTable) + " ip ON ip.PROVIDER_ALIAS = m.IDP_ALIAS AND ip.REALM_ID = m.REALM_ID" +
                            " WHERE c.NAME = ? AND ip.ORGANIZATION_ID IS NOT NULL")) {
                ps.setString(1, GROUP_TYPE);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String mapperId = rs.getString(1);
                        // VALUE is a CLOB and Oracle rejects a plain equality against one, so the group type is
                        // compared here instead of in the WHERE clause
                        String groupType = rs.getString(2);
                        String organizationId = rs.getString(3);

                        if (!ORGANIZATION_GROUP_TYPE.equals(groupType) || alreadyMigrated.contains(mapperId)) {
                            continue;
                        }

                        statements.add(new InsertStatement(null, null, mapperConfigTable)
                                .addColumnValue("IDP_MAPPER_ID", mapperId)
                                .addColumnValue("NAME", ORGANIZATION_ID)
                                .addColumnValue("VALUE", organizationId));
                        // a mapper cannot hold two groupType rows, but guard the (IDP_MAPPER_ID, NAME) primary key anyway
                        alreadyMigrated.add(mapperId);
                        count++;
                    }
                }
            }

            confirmationMessage.append("Recorded the target organization on " + count + " organization group IdP mappers.");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when populating IDP_MAPPER_CONFIG with '" + ORGANIZATION_ID + "'", e);
        }
    }

    private Set<String> getMappersWithOrganizationId(String mapperConfigTable) throws Exception {
        Set<String> result = new HashSet<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.IDP_MAPPER_ID FROM " + getTableName(mapperConfigTable) + " c WHERE c.NAME = ?")) {
            ps.setString(1, ORGANIZATION_ID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        }

        return result;
    }

    @Override
    protected String getTaskId() {
        return "Record the target organization on organization group IdP mappers";
    }
}