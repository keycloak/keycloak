package org.keycloak.it.provider.annotation;

import org.keycloak.it.TestProvider;

public class AbstractProviderFactoryTestProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { AbstractProviderFactory.class };
    }
}
