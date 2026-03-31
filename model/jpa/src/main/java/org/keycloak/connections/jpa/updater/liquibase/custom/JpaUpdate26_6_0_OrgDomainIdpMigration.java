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

import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.CustomChangeException;
import liquibase.statement.core.RawParameterizedSqlStatement;

public class JpaUpdate26_6_0_OrgDomainIdpMigration extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        String orgDomainTable = getTableName("ORG_DOMAIN");
        String identityProviderTable = getTableName("IDENTITY_PROVIDER");
        String identityProviderConfigTable = getTableName("IDENTITY_PROVIDER_CONFIG");

        if (database instanceof PostgresDatabase) {
            generateUpdateQueryForPostgreSQL(orgDomainTable, identityProviderTable, identityProviderConfigTable);
            return;
        }

        if (database instanceof MySQLDatabase) {
            // and MariaDB
            generateUpdateQueryForMySQL(orgDomainTable, identityProviderTable, identityProviderConfigTable);
            return;
        }

        if (database instanceof MSSQLDatabase) {
            generateUpdateQueryForMSSQL(orgDomainTable, identityProviderTable, identityProviderConfigTable);
            return;
        }

        if (database instanceof OracleDatabase) {
            generateUpdateQueryForOracle(orgDomainTable, identityProviderTable, identityProviderConfigTable);
            return;
        }

        // H2 and others, very slow with O(n^2) complexity
        // It is standard SQL queries, it *must* be compatible with all vendors (fingers crossed)
        generateUpdateQueryUsingStandardSQL(orgDomainTable, identityProviderTable, identityProviderConfigTable);
    }

    @Override
    protected String getTaskId() {
        return "Migrate IDP_ID in ORG_DOMAIN table based on IDP config";
    }

    private void generateUpdateQueryUsingStandardSQL(String orgDomainTable, String identityProviderTable, String identityProviderConfigTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE %s od
                SET od.idp_id = (
                    SELECT ip.internal_id 
                    FROM %s ip
                    INNER JOIN %s ipc ON ipc.name = 'kc.org.domain' AND ipc.value = od.name
                    WHERE ip.organization_id = od.org_id
                )
                WHERE od.idp_id IS NULL"""
                .formatted(orgDomainTable, identityProviderTable, identityProviderConfigTable)));
    }

    private void generateUpdateQueryForOracle(String orgDomainTable, String identityProviderTable, String identityProviderConfigTable) {
        statements.add(new RawParameterizedSqlStatement("""
                MERGE INTO %s od
                USING (
                    SELECT ip.internal_id, ipc.value as domain_name, ip.organization_id
                    FROM %s ip
                    INNER JOIN %s ipc ON ipc.name = 'kc.org.domain' AND ipc.identity_provider_id = ip.internal_id
                ) ip_data ON (od.name = ip_data.domain_name AND od.org_id = ip_data.organization_id)
                WHEN MATCHED THEN
                UPDATE SET od.idp_id = ip_data.internal_id"""
                .formatted(orgDomainTable, identityProviderTable, identityProviderConfigTable)));
    }

    private void generateUpdateQueryForMSSQL(String orgDomainTable, String identityProviderTable, String identityProviderConfigTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE od
                SET od.idp_id = ip.internal_id
                FROM %s od
                INNER JOIN %s ip ON ip.organization_id = od.org_id
                INNER JOIN %s ipc ON ipc.name = 'kc.org.domain' AND ipc.value = od.name AND ipc.identity_provider_id = ip.internal_id
                WHERE od.idp_id IS NULL"""
                .formatted(orgDomainTable, identityProviderTable, identityProviderConfigTable)));
    }

    private void generateUpdateQueryForMySQL(String orgDomainTable, String identityProviderTable, String identityProviderConfigTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE %s od
                INNER JOIN %s ip ON ip.organization_id = od.org_id
                INNER JOIN %s ipc ON ipc.name = 'kc.org.domain' AND ipc.value = od.name AND ipc.identity_provider_id = ip.internal_id
                SET od.idp_id = ip.internal_id
                WHERE od.idp_id IS NULL"""
                .formatted(orgDomainTable, identityProviderTable, identityProviderConfigTable)));
    }

    private void generateUpdateQueryForPostgreSQL(String orgDomainTable, String identityProviderTable, String identityProviderConfigTable) {
        statements.add(new RawParameterizedSqlStatement("""
                UPDATE %s od
                SET idp_id = ip.internal_id
                FROM %s ip
                INNER JOIN %s ipc ON ipc.name = 'kc.org.domain' AND ipc.identity_provider_id = ip.internal_id
                WHERE ip.organization_id = od.org_id AND ipc.value = od.name"""
                .formatted(orgDomainTable, identityProviderTable, identityProviderConfigTable)));
    }
}
