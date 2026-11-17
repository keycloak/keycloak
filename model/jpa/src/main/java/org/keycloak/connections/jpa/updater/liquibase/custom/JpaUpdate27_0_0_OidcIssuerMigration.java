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

import org.keycloak.migration.migrators.OidcIssuerMigration;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;
import org.jboss.logging.Logger;

public class JpaUpdate27_0_0_OidcIssuerMigration extends CustomKeycloakTask {

    private static final Logger LOG = Logger.getLogger(JpaUpdate27_0_0_OidcIssuerMigration.class);

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String sql = "SELECT p.INTERNAL_ID, p.PROVIDER_ID, p.PROVIDER_ALIAS, p.REALM_ID, c.VALUE FROM "
                + getTableName("IDENTITY_PROVIDER") + " p JOIN " + getTableName("IDENTITY_PROVIDER_CONFIG")
                + " c ON c.IDENTITY_PROVIDER_ID = p.INTERNAL_ID WHERE c.NAME = 'issuer' AND c.VALUE LIKE '%,%'";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString(1);
                String providerId = rs.getString(2);
                String alias = rs.getString(3);
                String realmId = rs.getString(4);
                String issuers = rs.getString(5);
                if (!OidcIssuerMigration.isMultiIssuerProvider(kcSession, providerId, issuers)) {
                    if (OidcIssuerMigration.getFactory(kcSession, providerId) == null) {
                        LOG.warnf("Identity provider '%s' in realm '%s' configures multiple issuers (%s), but its provider "
                                + "'%s' is not installed, so it cannot be migrated. If you install that provider later, "
                                + "replace the issuer list with a single issuer and configure a separate identity provider "
                                + "for each additional issuer.", alias, realmId, issuers, providerId);
                    }
                    continue;
                }
                String first = OidcIssuerMigration.firstIssuer(issuers);
                if (first != null) {
                    statements.add(new UpdateStatement(null, null, database.correctObjectName("IDENTITY_PROVIDER_CONFIG", Table.class))
                            .addNewColumnValue("VALUE", first)
                            .setWhereClause("IDENTITY_PROVIDER_ID=? AND NAME=?").addWhereParameter(id).addWhereParameter("issuer"));
                    LOG.warnf("Disabling identity provider '%s' in realm '%s' because it configured multiple issuers (%s). "
                            + "The same subject from different issuers could authenticate as the wrong user. Only the first issuer will be retained. "
                            + "Review existing federated links before enabling the provider. Configure a separate identity provider for each additional issuer; "
                            + "existing user links cannot be split automatically.",
                            alias, realmId, issuers);
                } else {
                    LOG.warnf("Disabling identity provider '%s' in realm '%s' because its issuer value (%s) "
                            + "contains commas but no valid issuer. Correct the issuer before enabling the provider.",
                            alias, realmId, issuers);
                }
                statements.add(new UpdateStatement(null, null, database.correctObjectName("IDENTITY_PROVIDER", Table.class))
                        .addNewColumnValue("ENABLED", false).setWhereClause("INTERNAL_ID=?").addWhereParameter(id));
            }
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Failed to migrate OIDC issuer lists", e);
        }
        confirmationMessage.append("Migrated OIDC issuer lists and disabled affected identity providers");
    }

    @Override
    protected String getTaskId() {
        return "27.0.0-54458-oidc-single-issuer";
    }
}
