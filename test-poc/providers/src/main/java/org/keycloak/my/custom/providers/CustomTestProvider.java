package org.keycloak.my.custom.providers;

import org.keycloak.it.TestProvider;

import java.util.Collections;
import java.util.Map;

public class CustomTestProvider implements TestProvider {

    @Override
    public String getName() {
        return "MyCustomRealmResourceProvider";
    }

    @Override
    public Class[] getClasses() {
        return new Class[] {MyCustomRealmResourceProvider.class, MyCustomRealmResourceProviderFactory.class};
    }

    @Override
    public Map<String, String> getManifestResources() {
        return Collections.singletonMap("org.keycloak.services.resource.RealmResourceProviderFactory", "services/org.keycloak.services.resource.RealmResourceProviderFactory");
    }
}
