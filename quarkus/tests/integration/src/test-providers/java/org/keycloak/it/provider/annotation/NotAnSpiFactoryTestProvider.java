package org.keycloak.it.provider.annotation;

import org.keycloak.it.TestProvider;

public class NotAnSpiFactoryTestProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { NotAnSpiFactoryProviderFactory.class };
    }
}
