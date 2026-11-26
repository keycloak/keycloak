package com.acme.provider.multitenancy;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;

@ApplicationScoped
@PersistenceUnitExtension("schema-store")
public class SchemaTenantResolver extends AbstractTenantResolver {

    @Override
    protected String defaultTenant() {
        // tenant identifiers for SCHEMA multi-tenancy are schema names; PUBLIC always exists on H2
        return "PUBLIC";
    }
}
