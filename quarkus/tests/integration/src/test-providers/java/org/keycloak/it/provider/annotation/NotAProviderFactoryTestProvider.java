package org.keycloak.it.provider.annotation;

import org.keycloak.it.TestProvider;

public class NotAProviderFactoryTestProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { NotAProviderFactory.class };
    }
}
