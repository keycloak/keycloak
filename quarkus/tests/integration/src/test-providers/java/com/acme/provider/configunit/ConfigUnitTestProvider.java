package com.acme.provider.configunit;

import java.util.Map;

import org.keycloak.it.TestProvider;

public class ConfigUnitTestProvider implements TestProvider {

    @Override
    public String getName() {
        return "config-unit-provider";
    }

    @Override
    public Class[] getClasses() {
        return new Class[] { ConfigUnitEntity.class, ConfigUnitResource.class, ConfigUnitResourceFactory.class };
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Map.of("org.keycloak.services.resource.RealmResourceProviderFactory", "services/org.keycloak.services.resource.RealmResourceProviderFactory");
    }
}
