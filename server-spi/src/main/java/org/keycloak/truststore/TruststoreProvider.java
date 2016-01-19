package org.keycloak.truststore;

import org.keycloak.provider.Provider;

import java.security.KeyStore;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public interface TruststoreProvider extends Provider {

    HostnameVerificationPolicy getPolicy();

    KeyStore getTruststore();
}
