package org.keycloak.connections.truststore;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
class TruststoreProviderSingleton {

    static private TruststoreProvider provider;

    static void set(TruststoreProvider tp) {
        provider = tp;
    }

    static TruststoreProvider get() {
        return provider;
    }
}
