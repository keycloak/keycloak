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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.DeleteStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

public class JpaUpdate26_8_0_DomainIdpRoutingMigration extends CustomKeycloakTask {

    // IdP internal id -> the organization it is linked to
    private final Map<String, IdpOrganizationLink> links = new HashMap<>();

    @Override
    protected boolean isApplicable() throws CustomChangeException {
        // A database that predates the organization tables has no organizations to migrate
        return super.isApplicable() && hasTable("ORG") && hasTable("ORG_DOMAIN");
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        // the links are not in IDENTITY_PROVIDER.ORGANIZATION_ID yet when the script is generated from a 25.x database
        for (IdpOrganizationLink link : getIdpOrganizationLinks()) {
            links.put(link.idpInternalId(), link);
        }
        populateOrgIdentityProvider();
        populateDomainIdpRouting();
        cleanupIdpConfig();
    }

    private void populateOrgIdentityProvider() {
        String orgIdpTable = database.correctObjectName("ORG_IDENTITY_PROVIDER", Table.class);

        for (IdpOrganizationLink link : links.values()) {
            statements.add(new InsertStatement(null, null, orgIdpTable)
                    .addColumnValue("ORGANIZATION_ID", link.organizationId())
                    .addColumnValue("IDENTITY_PROVIDER_ID", link.idpInternalId())
                    .addColumnValue("AUTO_MEMBERSHIP", true)
                    .addColumnValue("MEMBERSHIP_TYPE", "MANAGED"));
        }
        confirmationMessage.append("Migrated IdP-org links to ORG_IDENTITY_PROVIDER. ");
    }

    private void populateDomainIdpRouting() throws CustomChangeException {
        String ipcTable = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);
        String orgDomainTable = database.correctObjectName("ORG_DOMAIN", Table.class);
        String orgTable = database.correctObjectName("ORG", Table.class);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT c.IDENTITY_PROVIDER_ID, c.VALUE FROM " + getTableName(ipcTable) +
                        " c WHERE c.NAME = 'kc.org.domain'");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String idpInternalId = rs.getString(1);
                String domainValue = rs.getString(2);

                IdpOrganizationLink link = links.get(idpInternalId);
                if (link == null) {
                    // the IdP is not linked to an organization, so there is no domain to route
                    continue;
                }

                boolean autoRedirect = isEmailMatchEnabled(idpInternalId);

                if (!"ANY".equals(domainValue)) {
                    // Case A: specific domain name — look up ORG_DOMAIN.ID scoped by IdP's realm.
                    // Join through ORG instead of ORG_DOMAIN.REALM_ID: during manual migration script generation
                    // (migration-strategy=manual), this query runs against the pre-upgrade database where
                    // ORG_DOMAIN.REALM_ID does not exist yet, which would fail with "column does not exist".
                    try (PreparedStatement domLookup = connection.prepareStatement(
                            "SELECT dom.ID FROM " + getTableName(orgDomainTable) + " dom" +
                                    " JOIN " + getTableName(orgTable) + " o ON o.ID = dom.ORG_ID" +
                                    " WHERE dom.NAME = ? AND o.ID = ? AND o.REALM_ID = ?")) {
                        domLookup.setString(1, domainValue);
                        domLookup.setString(2, link.organizationId());
                        domLookup.setString(3, link.realmId());
                        try (ResultSet domRs = domLookup.executeQuery()) {
                            if (domRs.next()) {
                                String domainId = domRs.getString(1);
                                statements.add(new UpdateStatement(null, null, orgDomainTable)
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
                    setIdpForOrgDomains(link, excludedDomains, autoRedirect);
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

    private void setIdpForOrgDomains(IdpOrganizationLink link, Set<String> excludedDomains, boolean autoRedirect) throws Exception {
        String orgDomainTable = database.correctObjectName("ORG_DOMAIN", Table.class);
        String idpInternalId = link.idpInternalId();
        String orgId = link.organizationId();
        Set<String> existingDomainNames = new HashSet<>();

        // Get all domains for this org (ORG_ID is directly on ORG_DOMAIN).
        // Do not filter on ORG_DOMAIN.IDP_ID: that column is added by an earlier changeset of the same
        // release, so it is absent on the pre-upgrade database that a manual migration script
        // (migration-strategy=manual) is generated against. The updates are only queued here, so the
        // column holds no routing yet when this task runs during an automatic migration either.
        try (PreparedStatement domPs = connection.prepareStatement(
                "SELECT d.ID, d.NAME FROM " + getTableName(orgDomainTable) +
                        " d WHERE d.ORG_ID = ?")) {
            domPs.setString(1, orgId);
            try (ResultSet domRs = domPs.executeQuery()) {
                while (domRs.next()) {
                    String domainId = domRs.getString(1);
                    String domainName = domRs.getString(2);
                    existingDomainNames.add(domainName);
                    if (!excludedDomains.contains(domainName)) {
                        statements.add(new UpdateStatement(null, null, orgDomainTable)
                                .addNewColumnValue("IDP_ID", idpInternalId)
                                .addNewColumnValue("AUTO_REDIRECT", autoRedirect)
                                .setWhereClause("ID=?")
                                .addWhereParameter(domainId));
                    }
                }
            }
        }

        // Create unrouted ORG_DOMAIN rows for excluded patterns not already present
        for (String excludedPattern : excludedDomains) {
            if (existingDomainNames.contains(excludedPattern)) {
                continue;
            }
            statements.add(new InsertStatement(null, null, orgDomainTable)
                    .addColumnValue("ID", UUID.randomUUID().toString())
                    .addColumnValue("NAME", excludedPattern)
                    .addColumnValue("VERIFIED", false)
                    .addColumnValue("REALM_ID", link.realmId())
                    .addColumnValue("ORG_ID", orgId)
                    .addColumnValue("AUTO_REDIRECT", false));
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
