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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate26_8_0_DomainIdpRoutingMigration extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        populateOrgIdentityProvider();
        populateDomainIdpRouting();
        cleanupIdpConfig();
    }

    private void populateOrgIdentityProvider() throws CustomChangeException {
        String idpTable = database.correctObjectName("IDENTITY_PROVIDER", Table.class);
        String orgIdpTable = database.correctObjectName("ORG_IDENTITY_PROVIDER", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ip.ORGANIZATION_ID, ip.INTERNAL_ID FROM " + getTableName(idpTable) +
                        " ip WHERE ip.ORGANIZATION_ID IS NOT NULL");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String orgId = rs.getString(1);
                String idpInternalId = rs.getString(2);
                statements.add(new InsertStatement(null, null, orgIdpTable)
                        .addColumnValue("ORGANIZATION_ID", orgId)
                        .addColumnValue("IDENTITY_PROVIDER_ID", idpInternalId)
                        .addColumnValue("AUTO_MEMBERSHIP", true)
                        .addColumnValue("MEMBERSHIP_TYPE", "MANAGED"));
            }
            confirmationMessage.append("Migrated IdP-org links to ORG_IDENTITY_PROVIDER. ");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when populating ORG_IDENTITY_PROVIDER", e);
        }
    }

    private void populateDomainIdpRouting() throws CustomChangeException {
        String ipcTable = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);
        String idpTable = database.correctObjectName("IDENTITY_PROVIDER", Table.class);
        String domainTable = database.correctObjectName("DOMAIN", Table.class);
        String orgDomainTable = database.correctObjectName("ORG_DOMAIN", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.IDENTITY_PROVIDER_ID, c.VALUE FROM " + getTableName(ipcTable) +
                        " c WHERE c.NAME = 'kc.org.domain'");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String idpInternalId = rs.getString(1);
                String domainValue = rs.getString(2);

                boolean autoRedirect = isEmailMatchEnabled(idpInternalId);

                if (!"ANY".equals(domainValue)) {
                    // Case A: specific domain name — look up DOMAIN.ID scoped by IdP's realm
                    try (PreparedStatement domLookup = connection.prepareStatement(
                            "SELECT dom.ID FROM " + getTableName(domainTable) + " dom" +
                                    " JOIN " + getTableName(idpTable) + " ip ON dom.REALM_ID = ip.REALM_ID" +
                                    " WHERE dom.NAME = ? AND ip.INTERNAL_ID = ?")) {
                        domLookup.setString(1, domainValue);
                        domLookup.setString(2, idpInternalId);
                        try (ResultSet domRs = domLookup.executeQuery()) {
                            if (domRs.next()) {
                                String domainId = domRs.getString(1);
                                statements.add(new UpdateStatement(null, null, domainTable)
                                        .addNewColumnValue("IDP_ID", idpInternalId)
                                        .addNewColumnValue("AUTO_REDIRECT", autoRedirect)
                                        .setWhereClause("ID=?")
                                        .addWhereParameter(domainId));
                            }
                        }
                    }
                } else {
                    // Case B: ANY — set IDP_ID for all org domains not in exclusion list
                    Set<String> excludedDomains = getExcludedDomains(idpInternalId);
                    setIdpForOrgDomains(idpInternalId, excludedDomains, autoRedirect);
                }
            }
            confirmationMessage.append("Migrated domain-IdP routing to DOMAIN.IDP_ID.");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when populating DOMAIN.IDP_ID", e);
        }
    }

    private Set<String> getExcludedDomains(String idpInternalId) throws Exception {
        String ipcTable = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.VALUE FROM " + getTableName(ipcTable) +
                        " c WHERE c.IDENTITY_PROVIDER_ID = ? AND c.NAME = 'kc.org.excluded.domains'")) {
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

    private boolean isEmailMatchEnabled(String idpInternalId) throws Exception {
        String ipcTable = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.VALUE FROM " + getTableName(ipcTable) +
                        " c WHERE c.IDENTITY_PROVIDER_ID = ? AND c.NAME = 'kc.org.broker.redirect.mode.email-matches'")) {
            ps.setString(1, idpInternalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Boolean.parseBoolean(rs.getString(1));
                }
            }
        }
        return false;
    }

    private void setIdpForOrgDomains(String idpInternalId, Set<String> excludedDomains, boolean autoRedirect) throws Exception {
        String idpTable = database.correctObjectName("IDENTITY_PROVIDER", Table.class);
        String orgDomainTable = database.correctObjectName("ORG_DOMAIN", Table.class);
        String domainTable = database.correctObjectName("DOMAIN", Table.class);

        // Find orgs linked to this IdP — query IDENTITY_PROVIDER directly because
        // ORG_IDENTITY_PROVIDER inserts are deferred (not yet executed by Liquibase)
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ip.ORGANIZATION_ID, ip.REALM_ID FROM " + getTableName(idpTable) +
                        " ip WHERE ip.INTERNAL_ID = ? AND ip.ORGANIZATION_ID IS NOT NULL")) {
            ps.setString(1, idpInternalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String orgId = rs.getString(1);
                    String realmId = rs.getString(2);
                    Set<String> existingDomainNames = new HashSet<>();
                    Set<String> insertedDomains = new HashSet<>();

                    // Get all domains for this org
                    try (PreparedStatement domPs = connection.prepareStatement(
                            "SELECT dom.ID, dom.NAME FROM " + getTableName(domainTable) + " dom" +
                                    " JOIN " + getTableName(orgDomainTable) + " od ON od.DOMAIN_ID = dom.ID" +
                                    " WHERE od.ORG_ID = ? AND dom.IDP_ID IS NULL")) {
                        domPs.setString(1, orgId);
                        try (ResultSet domRs = domPs.executeQuery()) {
                            while (domRs.next()) {
                                String domainId = domRs.getString(1);
                                String domainName = domRs.getString(2);
                                existingDomainNames.add(domainName);
                                if (!excludedDomains.contains(domainName)) {
                                    statements.add(new UpdateStatement(null, null, domainTable)
                                            .addNewColumnValue("IDP_ID", idpInternalId)
                                            .addNewColumnValue("AUTO_REDIRECT", autoRedirect)
                                            .setWhereClause("ID=?")
                                            .addWhereParameter(domainId));
                                }
                            }
                        }
                    }

                    // Create unrouted DOMAIN rows for excluded patterns not already present
                    for (String excludedPattern : excludedDomains) {
                        if (existingDomainNames.contains(excludedPattern)) {
                            continue;
                        }

                        String domainId = null;

                        // Check if domain already exists in DOMAIN table for this realm
                        try (PreparedStatement checkDom = connection.prepareStatement(
                                "SELECT dom.ID FROM " + getTableName(domainTable) +
                                        " dom WHERE dom.NAME = ? AND dom.REALM_ID = ?")) {
                            checkDom.setString(1, excludedPattern);
                            checkDom.setString(2, realmId);
                            try (ResultSet checkRs = checkDom.executeQuery()) {
                                if (checkRs.next()) {
                                    domainId = checkRs.getString(1);
                                }
                            }
                        }

                        if (domainId == null && !insertedDomains.contains(excludedPattern)) {
                            domainId = UUID.randomUUID().toString();
                            statements.add(new InsertStatement(null, null, domainTable)
                                    .addColumnValue("ID", domainId)
                                    .addColumnValue("NAME", excludedPattern)
                                    .addColumnValue("VERIFIED", false)
                                    .addColumnValue("REALM_ID", realmId)
                                    .addColumnValue("AUTO_REDIRECT", false));
                            insertedDomains.add(excludedPattern);
                        }

                        if (domainId != null) {
                            // Check if already linked to this org
                            boolean alreadyLinked = false;
                            try (PreparedStatement checkLink = connection.prepareStatement(
                                    "SELECT 1 FROM " + getTableName(orgDomainTable) +
                                            " WHERE DOMAIN_ID = ? AND ORG_ID = ?")) {
                                checkLink.setString(1, domainId);
                                checkLink.setString(2, orgId);
                                try (ResultSet linkRs = checkLink.executeQuery()) {
                                    alreadyLinked = linkRs.next();
                                }
                            }

                            if (!alreadyLinked) {
                                statements.add(new InsertStatement(null, null, orgDomainTable)
                                        .addColumnValue("DOMAIN_ID", domainId)
                                        .addColumnValue("ORG_ID", orgId));
                            }
                        }
                    }
                }
            }
        }
    }

    private void cleanupIdpConfig() {
        String ipcTable = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);
        statements.add(new DeleteStatement(null, null, ipcTable)
                .setWhere("NAME=?")
                .addWhereParameter("kc.org.domain"));
        statements.add(new DeleteStatement(null, null, ipcTable)
                .setWhere("NAME=?")
                .addWhereParameter("kc.org.excluded.domains"));
        statements.add(new DeleteStatement(null, null, ipcTable)
                .setWhere("NAME=?")
                .addWhereParameter("kc.org.broker.redirect.mode.email-matches"));
        confirmationMessage.append("Cleaned up kc.org.domain, kc.org.excluded.domains, and kc.org.broker.redirect.mode.email-matches config entries.");
    }

    @Override
    protected String getTaskId() {
        return "Migrate IdP-org links and domain-IdP routing to normalized tables";
    }
}
