package org.keycloak.testsuite.performance.web;

import org.keycloak.models.KeycloakSessionFactory;

/**
 * Static holder to allow sharing ProviderSessionFactory among different JAX-RS applications
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakSessionFactoryHolder {

    private static KeycloakSessionFactory keycloakSessionFactory;

    public static KeycloakSessionFactory getKeycloakSessionFactory() {
        return keycloakSessionFactory;
    }

    public static void setKeycloakSessionFactory(KeycloakSessionFactory keycloakSessionFactory) {
        KeycloakSessionFactoryHolder.keycloakSessionFactory = keycloakSessionFactory;
    }
}
