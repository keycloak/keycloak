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
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

/**
 * Converts the audience mappers that the CIMD executor added to clients for the resources it accepted. They were of
 * the generic audience mapper type, which adds the audience regardless of the resource indicator allow list, and
 * become of the type that adds the audience only while the resource is allowed for the client.
 *
 * <p>The mappers are identified in the same way as the CIMD executor identified them: by their type and by the prefix
 * of their name, both compared case-sensitively. A {@code LIKE} condition alone is not enough, because it is
 * case-insensitive with the collations of some databases, and would also convert unrelated audience mappers whose
 * name differs from the prefix only in case.
 */
public class JpaUpdate27_0_0_CimdResourceIndicatorAudienceMapper extends CustomKeycloakTask {

    private static final String AUDIENCE_MAPPER = "oidc-audience-mapper";
    private static final String CIMD_RESOURCE_INDICATOR_AUDIENCE_MAPPER = "oidc-cimd-resource-indicator-audience-mapper";
    private static final String NAME_PREFIX = "cimd-resource-indicator-";

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String protocolMapperTable = getTableName("PROTOCOL_MAPPER");
        // the conditions only narrow down the candidates: the type and the name are checked below.
        String sql = "SELECT ID, NAME, PROTOCOL_MAPPER_NAME FROM " + protocolMapperTable
                + " WHERE PROTOCOL_MAPPER_NAME = ? AND NAME LIKE ? AND CLIENT_ID IS NOT NULL";
        int converted = 0;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, AUDIENCE_MAPPER);
            ps.setString(2, NAME_PREFIX + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    String name = rs.getString(2);
                    String protocolMapper = rs.getString(3);
                    if (name == null || !name.startsWith(NAME_PREFIX) || !AUDIENCE_MAPPER.equals(protocolMapper)) {
                        continue;
                    }
                    statements.add(new UpdateStatement(null, null, database.correctObjectName("PROTOCOL_MAPPER", Table.class))
                            .addNewColumnValue("PROTOCOL_MAPPER_NAME", CIMD_RESOURCE_INDICATOR_AUDIENCE_MAPPER)
                            .setWhereClause("ID=?").addWhereParameter(id));
                    converted++;
                }
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Failed to convert the audience mappers added by the CIMD executor", e);
        }
        confirmationMessage.append("Converted " + converted + " audience mappers added by the CIMD executor");
    }

    @Override
    protected String getTaskId() {
        return "27.0.0-55131-cimd-resource-indicator-audience-mapper";
    }
}
