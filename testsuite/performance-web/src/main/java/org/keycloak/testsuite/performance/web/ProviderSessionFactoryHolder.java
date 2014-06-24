package org.keycloak.testsuite.performance.web;

import org.keycloak.provider.ProviderSessionFactory;

/**
 * Static holder to allow sharing ProviderSessionFactory among different JAX-RS applications
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProviderSessionFactoryHolder {

    private static ProviderSessionFactory providerSessionFactory;

    public static ProviderSessionFactory getProviderSessionFactory() {
        return providerSessionFactory;
    }

    public static void setProviderSessionFactory(ProviderSessionFactory providerSessionFactory) {
        ProviderSessionFactoryHolder.providerSessionFactory = providerSessionFactory;
    }
}
