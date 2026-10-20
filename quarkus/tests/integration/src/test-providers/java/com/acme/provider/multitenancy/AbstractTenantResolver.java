package com.acme.provider.multitenancy;

import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;

abstract class AbstractTenantResolver implements TenantResolver {

    protected abstract String defaultTenant();

    @Override
    public String getDefaultTenantId() {
        String tenant = TenantContext.get();
        return tenant != null ? tenant : defaultTenant();
    }

    @Override
    public String resolveTenantId() {
        String tenant = TenantContext.get();
        return tenant != null ? tenant : defaultTenant();
    }
}
