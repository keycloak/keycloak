package com.acme.provider.multitenancy;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;

@ApplicationScoped
@PersistenceUnitExtension("disc-store")
public class DiscriminatorTenantResolver extends AbstractTenantResolver {

    @Override
    protected String defaultTenant() {
        return "tenant-1";
    }
}
