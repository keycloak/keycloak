package org.keycloak.testframework.tests.providers.single;

import org.keycloak.services.resource.RealmResourceProvider;

public class ProviderWithResourceProvider implements RealmResourceProvider {

    @Override
    public ProviderWithResourceResource getResource() {
        return new ProviderWithResourceResource();
    }

    @Override
    public void close() {

    }

}
