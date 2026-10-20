package com.acme.provider.multitenancy;

import java.util.Map;

import org.keycloak.it.TestProvider;

public class MultiTenancyTestProvider implements TestProvider {

    @Override
    public String getName() {
        return "multi-tenancy-provider";
    }

    @Override
    public Class[] getClasses() {
        return new Class[] {
                TenantContext.class,
                AbstractTenantResolver.class,
                DatabaseTenantResolver.class,
                SchemaTenantResolver.class,
                DiscriminatorTenantResolver.class,
                DatabaseTenantEntity.class,
                SchemaTenantEntity.class,
                DiscriminatorTenantEntity.class,
                MultiTenancyResource.class,
                MultiTenancyResourceFactory.class
        };
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Map.of(
                "persistence.xml", "persistence.xml",
                "org.keycloak.services.resource.RealmResourceProviderFactory", "services/org.keycloak.services.resource.RealmResourceProviderFactory");
    }
}
