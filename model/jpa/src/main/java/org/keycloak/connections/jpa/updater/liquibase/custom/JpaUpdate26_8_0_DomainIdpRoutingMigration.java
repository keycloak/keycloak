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
import java.sql.SQLException;
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

import liquibase.datatype.DataTypeFactory;
import liquibase.exception.CustomChangeException;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.DeleteStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;
import org.jboss.logging.Logger;

public class JpaUpdate26_8_0_DomainIdpRoutingMigration extends CustomKeycloakTask {

    private static final Logger logger = Logger.getLogger(JpaUpdate26_8_0_DomainIdpRoutingMigration.class);

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
                        domains.getOrDefault(realmId, List.of()), entry.getValue(), true);

                for (Routing routing : result.routings()) {
                    statements.add(new UpdateStatement(null, null, orgDomainTable)
                            .addNewColumnValue("IDP_ID", routing.identityProvider().id())
                            .addNewColumnValue("AUTO_REDIRECT", routing.identityProvider().autoRedirect())
                            .setWhereClause("ID=?")
                            .addWhereParameter(realmDomainIds.get(routing.name())));
                }

                // domains for excluded domains, routed to the identity provider that redirected their emails, if any
                for (Routing domain : result.newDomains()) {
                    String existing = findDomainWithSameName(orgDomainTable, realmId, domain.name());
                    if (existing != null) {
                        logger.warnf("Excluded domain '%s' of organization '%s' is not migrated: the database treats it as the same name as the domain '%s'. Review the domains of organization '%s'",
                                domain.name(), organizationNames.get(domain.organizationId()), existing, organizationNames.get(domain.organizationId()));
                        continue;
                    }
                    statements.add(insertDomainIfAbsent(orgDomainTable, orgTable, domain));
                }
            }
            confirmationMessage.append("Migrated domain-IdP routing to DOMAIN.IDP_ID.");
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when populating DOMAIN.IDP_ID", e);
        }
    }

    /**
     * The name of a domain of the realm that the database treats as the same name, as the collation of the column
     * compares names, or {@code null}.
     */
    private String findDomainWithSameName(String orgDomainTable, String realmId, String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT d.NAME FROM " + getTableName(orgDomainTable) + " d WHERE d.REALM_ID = ? AND d.NAME = ?")) {
            ps.setString(1, realmId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Inserts the domain unless the realm already has a domain with the same name, as the collation of the column
     * compares names: two new domains can have names that the collation treats as the same, and adding both would
     * violate UK_ORG_DOMAIN_REALM_NAME.
     */
    private SqlStatement insertDomainIfAbsent(String orgDomainTable, String orgTable, Routing domain) {
        IdentityProvider idp = domain.identityProvider();
        String table = getTableName(orgDomainTable);
        String name = toSql(domain.name());

        return new RawSqlStatement("INSERT INTO " + table + " (ID, NAME, VERIFIED, REALM_ID, ORG_ID, AUTO_REDIRECT" + (idp == null ? "" : ", IDP_ID") + ")"
                + " SELECT " + toSql(UUID.randomUUID().toString()) + ", " + name + ", " + toSql(false) + ", o.REALM_ID, o.ID, "
                + toSql(idp != null && idp.autoRedirect()) + (idp == null ? "" : ", " + toSql(idp.id()))
                + " FROM " + getTableName(orgTable) + " o WHERE o.ID = " + toSql(domain.organizationId())
                + " AND NOT EXISTS (SELECT 1 FROM " + table + " d WHERE d.REALM_ID = o.REALM_ID AND d.NAME = " + name + ")");
    }

    private String toSql(Object value) {
        return DataTypeFactory.getInstance().fromObject(value, database).objectToSql(value, database);
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
