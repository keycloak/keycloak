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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.migration.migrators.DomainIdpRoutingMigration;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Domain;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.IdentityProvider;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Result;
import org.keycloak.migration.migrators.DomainIdpRoutingMigration.Routing;
import org.keycloak.migration.migrators.MigrationUtils;

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
        String orgTable = database.correctObjectName("ORG", Table.class);
        String orgDomainTable = database.correctObjectName("ORG_DOMAIN", Table.class);
        String idpTable = database.correctObjectName("IDENTITY_PROVIDER", Table.class);
        String ipcTable = database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class);

        try {
            Map<String, String> organizationNames = new HashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT o.ID, o.NAME FROM " + getTableName(orgTable) + " o");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    organizationNames.put(rs.getString(1), rs.getString(2));
                }
            }

            // realm ID -> domain name -> ORG_DOMAIN.ID
            Map<String, Map<String, String>> domainIds = new HashMap<>();
            Map<String, List<Domain>> domains = new HashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT d.ID, d.NAME, d.ORG_ID, d.REALM_ID FROM " + getTableName(orgDomainTable) + " d");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String realmId = rs.getString(4);
                    domainIds.computeIfAbsent(realmId, id -> new HashMap<>()).put(rs.getString(2), rs.getString(1));
                    domains.computeIfAbsent(realmId, id -> new ArrayList<>()).add(new Domain(rs.getString(3), rs.getString(2)));
                }
            }

            // identity provider ID -> config name -> value
            Map<String, Map<String, String>> configs = new HashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT c.IDENTITY_PROVIDER_ID, c.NAME, c.VALUE FROM " + getTableName(ipcTable) +
                            " c WHERE c.NAME IN (?, ?, ?)")) {
                ps.setString(1, MigrationUtils.ORGANIZATION_DOMAIN_ATTRIBUTE);
                ps.setString(2, MigrationUtils.ORGANIZATION_EXCLUDED_DOMAIN_ATTRIBUTE);
                ps.setString(3, MigrationUtils.ORGANIZATION_REDIRECT_MODE_ATTRIBUTE);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        configs.computeIfAbsent(rs.getString(1), id -> new HashMap<>()).put(rs.getString(2), rs.getString(3));
                    }
                }
            }

            // query IDENTITY_PROVIDER directly because ORG_IDENTITY_PROVIDER inserts are deferred (not yet executed by Liquibase)
            Map<String, List<IdentityProvider>> identityProviders = new HashMap<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT ip.INTERNAL_ID, ip.PROVIDER_ALIAS, ip.ORGANIZATION_ID, ip.REALM_ID FROM " + getTableName(idpTable) +
                            " ip WHERE ip.ORGANIZATION_ID IS NOT NULL");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idpInternalId = rs.getString(1);
                    Map<String, String> config = configs.getOrDefault(idpInternalId, Map.of());
                    identityProviders.computeIfAbsent(rs.getString(4), id -> new ArrayList<>()).add(new IdentityProvider(
                            idpInternalId, rs.getString(2), rs.getString(3),
                            config.get(MigrationUtils.ORGANIZATION_DOMAIN_ATTRIBUTE),
                            config.get(MigrationUtils.ORGANIZATION_EXCLUDED_DOMAIN_ATTRIBUTE),
                            Boolean.parseBoolean(config.get(MigrationUtils.ORGANIZATION_REDIRECT_MODE_ATTRIBUTE))));
                }
            }

            for (Map.Entry<String, List<IdentityProvider>> entry : identityProviders.entrySet()) {
                String realmId = entry.getKey();
                Map<String, String> realmDomainIds = domainIds.getOrDefault(realmId, Map.of());
                Result result = DomainIdpRoutingMigration.migrate(organizationNames,
                        domains.getOrDefault(realmId, List.of()), entry.getValue());

                for (Routing routing : result.routings()) {
                    statements.add(new UpdateStatement(null, null, orgDomainTable)
                            .addNewColumnValue("IDP_ID", routing.identityProvider().id())
                            .addNewColumnValue("AUTO_REDIRECT", routing.identityProvider().autoRedirect())
                            .setWhereClause("ID=?")
                            .addWhereParameter(realmDomainIds.get(routing.name())));
                }

                // domains for excluded domains, routed to the identity provider that redirected their emails, if any
                for (Routing domain : result.newDomains()) {
                    IdentityProvider idp = domain.identityProvider();
                    InsertStatement insert = new InsertStatement(null, null, orgDomainTable)
                            .addColumnValue("ID", UUID.randomUUID().toString())
                            .addColumnValue("NAME", domain.name())
                            .addColumnValue("VERIFIED", false)
                            .addColumnValue("REALM_ID", realmId)
                            .addColumnValue("ORG_ID", domain.organizationId())
                            .addColumnValue("AUTO_REDIRECT", idp != null && idp.autoRedirect());
                    if (idp != null) {
                        insert.addColumnValue("IDP_ID", idp.id());
                    }
                    statements.add(insert);
                }
            }
            confirmationMessage.append("Migrated domain-IdP routing to DOMAIN.IDP_ID.");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when populating DOMAIN.IDP_ID", e);
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
