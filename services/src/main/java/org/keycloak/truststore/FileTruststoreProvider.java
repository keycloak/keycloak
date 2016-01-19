package org.keycloak.truststore;

import org.keycloak.truststore.HostnameVerificationPolicy;
import org.keycloak.truststore.TruststoreProvider;

import java.security.KeyStore;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FileTruststoreProvider implements TruststoreProvider {

    private final HostnameVerificationPolicy policy;
    private final KeyStore truststore;

    FileTruststoreProvider(KeyStore truststore, HostnameVerificationPolicy policy) {
        this.policy = policy;
        this.truststore = truststore;
    }

    @Override
    public HostnameVerificationPolicy getPolicy() {
        return policy;
    }

    @Override
    public KeyStore getTruststore() {
        return truststore;
    }

    @Override
    public void close() {
    }
}
