package org.keycloak.it.provider.annotation;

import org.keycloak.it.TestProvider;

/**
 * Installs {@link AnnotatedSocialIdentityProviderFactory} into the distribution without any
 * {@code META-INF/services} descriptor.
 */
public class AnnotatedSocialIdentityProviderTestProvider implements TestProvider {

    @Override
    public Class[] getClasses() {
        return new Class[] { AnnotatedSocialIdentityProviderFactory.class };
    }
}
