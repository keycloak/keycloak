package org.keycloak.connections.jpa.updater.liquibase;

import liquibase.database.core.CockroachDatabase;

/**
 * Liquibase's {@link CockroachDatabase} reports no DDL support in transactions and then runs the migration in auto-commit mode, which doesn't allow the savepoints used by Keycloak's custom tasks.
 * CockroachDB commits an open transaction before a DDL statement when {@code autocommit_before_ddl} is enabled, so Keycloak can keep using transactions.
 */
public class KeycloakCockroachDatabase extends CockroachDatabase {

    @Override
    public boolean supportsDDLInTransaction() {
        return true;
    }

}
