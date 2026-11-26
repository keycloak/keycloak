package com.acme.provider.singlenamed;

import java.util.Map;
import org.keycloak.it.TestProvider;

public class SingleNamedPuTestProvider implements TestProvider {
    @Override
    public String getName() {
        return "single-named-pu-provider";
    }

    @Override
    public Class[] getClasses() {
        return new Class[] { SingleNamedPuEntity.class };
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Map.of("persistence.xml", "persistence.xml", "orm.xml", "orm.xml");
    }
}
