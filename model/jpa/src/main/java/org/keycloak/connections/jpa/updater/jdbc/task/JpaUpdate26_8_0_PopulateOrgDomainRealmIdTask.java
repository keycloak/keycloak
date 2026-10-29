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
import java.util.List;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_8_0_PopulateOrgDomainRealmId}.
 * Populates REALM_ID in ORG_DOMAIN from the joined ORG table.
 */
public class JpaUpdate26_8_0_PopulateOrgDomainRealmIdTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Populate ORG_DOMAIN.REALM_ID from ORG.REALM_ID";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        String orgDomainTable = NativeSqlHelper.qualified("ORG_DOMAIN", defaultSchema);
        String orgTable = NativeSqlHelper.qualified("ORG", defaultSchema);
        List<String> stmts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT d.ID, o.REALM_ID FROM " + orgDomainTable
                        + " d JOIN " + orgTable + " o ON d.ORG_ID = o.ID WHERE d.REALM_ID IS NULL");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String domainId = rs.getString(1);
                String realmId = rs.getString(2);
                stmts.add("UPDATE " + orgDomainTable
                        + " SET REALM_ID = " + NativeSqlHelper.sqlLiteral(realmId)
                        + " WHERE ID = " + NativeSqlHelper.sqlLiteral(domainId));
            }
        }
        return stmts;
    }
}
