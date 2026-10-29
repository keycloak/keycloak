package com.acme.provider.tenantunit;

import java.util.Map;

import org.keycloak.it.TestProvider;

public class TenantUnitTestProvider implements TestProvider {

    @Override
    public String getName() {
        return "tenant-unit-provider";
    }

    @Override
    public Class[] getClasses() {
        return new Class[] { TenantUnitEntity.class, TenantUnitContext.class, TenantUnitTenantResolver.class, TenantUnitResource.class,
                TenantUnitResourceFactory.class };
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Map.of("org.keycloak.services.resource.RealmResourceProviderFactory", "services/org.keycloak.services.resource.RealmResourceProviderFactory");
    }
}
