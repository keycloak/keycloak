/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_8_0_IdpMapperOrganizationId}.
 * Records the target organization on every organization group IdP mapper.
 */
public class JpaUpdate26_8_0_IdpMapperOrganizationIdTask implements JpaMigrationTask {

    private static final String GROUP_TYPE = "groupType";
    private static final String ORGANIZATION_ID = "orgId";
    private static final String ORGANIZATION_GROUP_TYPE = "ORGANIZATION";

    @Override
    public String getTaskId() {
        return "Record the target organization on organization group IdP mappers";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String mapperConfigTable = NativeSqlHelper.qualified("IDP_MAPPER_CONFIG", defaultSchema);
        String mapperTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER_MAPPER", defaultSchema);
        String idpTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER", defaultSchema);
        List<String> stmts = new ArrayList<>();

        Set<String> alreadyMigrated = getMappersWithOrganizationId(connection, mapperConfigTable);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.IDP_MAPPER_ID, c.VALUE, ip.ORGANIZATION_ID FROM " + mapperConfigTable + " c"
                        + " JOIN " + mapperTable + " m ON m.ID = c.IDP_MAPPER_ID"
                        + " JOIN " + idpTable + " ip ON ip.PROVIDER_ALIAS = m.IDP_ALIAS AND ip.REALM_ID = m.REALM_ID"
                        + " WHERE c.NAME = ? AND ip.ORGANIZATION_ID IS NOT NULL")) {
            ps.setString(1, GROUP_TYPE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String mapperId = rs.getString(1);
                    String groupType = rs.getString(2);
                    String organizationId = rs.getString(3);

                    if (!ORGANIZATION_GROUP_TYPE.equals(groupType) || alreadyMigrated.contains(mapperId)) {
                        continue;
                    }
                    stmts.add("INSERT INTO " + mapperConfigTable
                            + " (IDP_MAPPER_ID, NAME, VALUE) VALUES ("
                            + NativeSqlHelper.sqlLiteral(mapperId) + ", "
                            + NativeSqlHelper.sqlLiteral(ORGANIZATION_ID) + ", "
                            + NativeSqlHelper.sqlLiteral(organizationId) + ")");
                    alreadyMigrated.add(mapperId);
                }
            }
        }
        return stmts;
    }

    private Set<String> getMappersWithOrganizationId(Connection conn, String mapperConfigTable)
            throws SQLException {
        Set<String> result = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.IDP_MAPPER_ID FROM " + mapperConfigTable + " c WHERE c.NAME = ?")) {
            ps.setString(1, ORGANIZATION_ID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        }
        return result;
    }
}
