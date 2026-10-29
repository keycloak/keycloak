package com.acme.provider.tenantunit;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;

/**
 * Resolves the tenants of the persistence unit of the {@code tenant-store} datasource. With the DATABASE strategy, the
 * tenant identifiers are the names of the datasources of the tenants.
 */
@ApplicationScoped
@PersistenceUnitExtension(TenantUnitTenantResolver.UNIT)
public class TenantUnitTenantResolver implements TenantResolver {

    public static final String UNIT = "tenant-store";
    public static final String DEFAULT_TENANT = "tenant-a";

    @Override
    public String getDefaultTenantId() {
        return DEFAULT_TENANT;
    }

    @Override
    public String resolveTenantId() {
        String tenant = TenantUnitContext.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }
}
