package org.keycloak.it.provider.annotation;

import org.keycloak.it.TestProvider;

public class NoPublicConstructorTestProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { NoPublicConstructorProviderFactory.class };
    }
}
