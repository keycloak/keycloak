package com.acme.provider.multitenancy;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;

@ApplicationScoped
@PersistenceUnitExtension("database-store")
public class DatabaseTenantResolver extends AbstractTenantResolver {

    @Override
    protected String defaultTenant() {
        // tenant identifiers for DATABASE multi-tenancy are datasource names; this one is always enabled in the tests
        return "mem-tenant";
    }
}
