package com.acme.provider.standalone;

import org.keycloak.it.TestProvider;
import java.util.Map;

public class StandaloneOrmXmlTestProvider implements TestProvider {
    @Override
    public String getName() {
        return "standalone-orm-provider";
    }

    @Override
    public Class[] getClasses() {
        return new Class[] { StandaloneEntity.class };
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Map.of("orm.xml", "orm.xml");
    }
}
