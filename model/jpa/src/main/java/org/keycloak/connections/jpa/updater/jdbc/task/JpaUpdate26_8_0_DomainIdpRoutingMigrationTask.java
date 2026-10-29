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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.keycloak.connections.jpa.updater.jdbc.JpaMigrationTask;
import org.keycloak.connections.jpa.updater.jdbc.NativeSqlHelper;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Migration task registered for the historic manifest class name {@code JpaUpdate26_8_0_DomainIdpRoutingMigration}.
 * Migrates IdP-org links and domain-IdP routing to normalized tables.
 */
public class JpaUpdate26_8_0_DomainIdpRoutingMigrationTask implements JpaMigrationTask {

    @Override
    public String getTaskId() {
        return "Migrate IdP-org links and domain-IdP routing to normalized tables";
    }

    @Override
    public List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException {
        List<String> stmts = new ArrayList<>();
        populateOrgIdentityProvider(connection, dialect, defaultSchema, stmts);
        populateDomainIdpRouting(connection, dialect, defaultSchema, stmts);
        cleanupIdpConfig(defaultSchema, stmts);
        return stmts;
    }

    private void populateOrgIdentityProvider(Connection conn, SqlDialect dialect, String schema, List<String> stmts)
            throws SQLException {
        String idpTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER", schema);
        String orgIdpTable = NativeSqlHelper.qualified("ORG_IDENTITY_PROVIDER", schema);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ip.ORGANIZATION_ID, ip.INTERNAL_ID FROM " + idpTable
                        + " ip WHERE ip.ORGANIZATION_ID IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String orgId = rs.getString(1);
                String idpInternalId = rs.getString(2);
                stmts.add("INSERT INTO " + orgIdpTable
                        + " (ORGANIZATION_ID, IDENTITY_PROVIDER_ID, AUTO_MEMBERSHIP, MEMBERSHIP_TYPE) VALUES ("
                        + NativeSqlHelper.sqlLiteral(orgId) + ", "
                        + NativeSqlHelper.sqlLiteral(idpInternalId) + ", "
                        + NativeSqlHelper.trueLiteral(dialect) + ", 'MANAGED')");
            }
        }
    }

    private void populateDomainIdpRouting(Connection conn, SqlDialect dialect, String schema, List<String> stmts)
            throws SQLException {
        String ipcTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER_CONFIG", schema);
        String idpTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER", schema);
        String orgDomainTable = NativeSqlHelper.qualified("ORG_DOMAIN", schema);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.IDENTITY_PROVIDER_ID, c.VALUE FROM " + ipcTable
                        + " c WHERE c.NAME = 'kc.org.domain'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String idpInternalId = rs.getString(1);
                String domainValue = rs.getString(2);
                boolean autoRedirect = isEmailMatchEnabled(conn, schema, idpInternalId);

                if (!"ANY".equals(domainValue)) {
                    // Specific domain name — look up ORG_DOMAIN.ID
                    try (PreparedStatement domLookup = conn.prepareStatement(
                            "SELECT dom.ID FROM " + orgDomainTable + " dom"
                                    + " JOIN " + idpTable + " ip ON dom.REALM_ID = ip.REALM_ID"
                                    + " AND dom.ORG_ID = ip.ORGANIZATION_ID"
                                    + " WHERE dom.NAME = ? AND ip.INTERNAL_ID = ?")) {
                        domLookup.setString(1, domainValue);
                        domLookup.setString(2, idpInternalId);
                        try (ResultSet domRs = domLookup.executeQuery()) {
                            if (domRs.next()) {
                                String domainId = domRs.getString(1);
                                stmts.add("UPDATE " + orgDomainTable
                                        + " SET IDP_ID = " + NativeSqlHelper.sqlLiteral(idpInternalId)
                                        + ", AUTO_REDIRECT = " + NativeSqlHelper.sqlLiteral(autoRedirect, dialect)
                                        + " WHERE ID = " + NativeSqlHelper.sqlLiteral(domainId));
                            }
                        }
                    }
                } else {
                    // ANY — set IDP_ID for all org domains not excluded
                    Set<String> excludedDomains = getExcludedDomains(conn, schema, idpInternalId);
                    setIdpForOrgDomains(conn, dialect, schema, idpInternalId, excludedDomains, autoRedirect, stmts);
                }
            }
        }
    }

    private Set<String> getExcludedDomains(Connection conn, String schema, String idpInternalId)
            throws SQLException {
        String ipcTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER_CONFIG", schema);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.VALUE FROM " + ipcTable
                        + " c WHERE c.IDENTITY_PROVIDER_ID = ? AND c.NAME = 'kc.org.excluded.domains'")) {
            ps.setString(1, idpInternalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString(1);
                    if (value != null && !value.isEmpty()) {
                        Set<String> result = new HashSet<>();
                        for (String d : value.split(",")) {
                            String trimmed = d.trim();
                            if (!trimmed.isEmpty()) {
                                result.add(trimmed);
                            }
                        }
                        return result;
                    }
                }
            }
        }
        return Collections.emptySet();
    }

    private boolean isEmailMatchEnabled(Connection conn, String schema, String idpInternalId)
            throws SQLException {
        String ipcTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER_CONFIG", schema);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.VALUE FROM " + ipcTable
                        + " c WHERE c.IDENTITY_PROVIDER_ID = ?"
                        + " AND c.NAME = 'kc.org.broker.redirect.mode.email-matches'")) {
            ps.setString(1, idpInternalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Boolean.parseBoolean(rs.getString(1));
                }
            }
        }
        return false;
    }

    private void setIdpForOrgDomains(Connection conn, SqlDialect dialect, String schema, String idpInternalId,
            Set<String> excludedDomains, boolean autoRedirect, List<String> stmts) throws SQLException {
        String idpTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER", schema);
        String orgDomainTable = NativeSqlHelper.qualified("ORG_DOMAIN", schema);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ip.ORGANIZATION_ID, ip.REALM_ID FROM " + idpTable
                        + " ip WHERE ip.INTERNAL_ID = ? AND ip.ORGANIZATION_ID IS NOT NULL")) {
            ps.setString(1, idpInternalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String orgId = rs.getString(1);
                    String realmId = rs.getString(2);
                    Set<String> existingDomainNames = new HashSet<>();
                    Set<String> insertedDomains = new HashSet<>();

                    try (PreparedStatement domPs = conn.prepareStatement(
                            "SELECT d.ID, d.NAME FROM " + orgDomainTable
                                    + " d WHERE d.ORG_ID = ? AND d.IDP_ID IS NULL")) {
                        domPs.setString(1, orgId);
                        try (ResultSet domRs = domPs.executeQuery()) {
                            while (domRs.next()) {
                                String domainId = domRs.getString(1);
                                String domainName = domRs.getString(2);
                                existingDomainNames.add(domainName);
                                if (!excludedDomains.contains(domainName)) {
                                    stmts.add("UPDATE " + orgDomainTable
                                            + " SET IDP_ID = " + NativeSqlHelper.sqlLiteral(idpInternalId)
                                            + ", AUTO_REDIRECT = " + NativeSqlHelper.sqlLiteral(autoRedirect, dialect)
                                            + " WHERE ID = " + NativeSqlHelper.sqlLiteral(domainId));
                                }
                            }
                        }
                    }

                    // Create ORG_DOMAIN rows for excluded patterns not already present
                    for (String excludedPattern : excludedDomains) {
                        if (existingDomainNames.contains(excludedPattern)) {
                            continue;
                        }
                        if (!insertedDomains.contains(excludedPattern)) {
                            String domainId = UUID.randomUUID().toString();
                            stmts.add("INSERT INTO " + orgDomainTable
                                    + " (ID, NAME, VERIFIED, REALM_ID, ORG_ID, AUTO_REDIRECT) VALUES ("
                                    + NativeSqlHelper.sqlLiteral(domainId) + ", "
                                    + NativeSqlHelper.sqlLiteral(excludedPattern) + ", "
                                    + NativeSqlHelper.sqlLiteral(false, dialect) + ", "
                                    + NativeSqlHelper.sqlLiteral(realmId) + ", "
                                    + NativeSqlHelper.sqlLiteral(orgId) + ", "
                                    + NativeSqlHelper.sqlLiteral(false, dialect) + ")");
                            insertedDomains.add(excludedPattern);
                        }
                    }
                }
            }
        }
    }

    private void cleanupIdpConfig(String schema, List<String> stmts) {
        String ipcTable = NativeSqlHelper.qualified("IDENTITY_PROVIDER_CONFIG", schema);
        stmts.add("DELETE FROM " + ipcTable + " WHERE NAME = 'kc.org.domain'");
        stmts.add("DELETE FROM " + ipcTable + " WHERE NAME = 'kc.org.excluded.domains'");
        stmts.add("DELETE FROM " + ipcTable + " WHERE NAME = 'kc.org.broker.redirect.mode.email-matches'");
    }
}
