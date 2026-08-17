package org.keycloak.it.provider.annotation;

import org.keycloak.it.TestProvider;

/**
 * Installs {@link AnnotatedEventListenerProviderFactory} into the distribution without any
 * {@code META-INF/services} descriptor.
 */
public class AnnotatedEventListenerTestProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { AnnotatedEventListenerProviderFactory.class };
    }
}
