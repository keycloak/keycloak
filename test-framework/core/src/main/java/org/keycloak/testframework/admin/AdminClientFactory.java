package org.keycloak.testframework.admin;

import org.apache.http.ssl.SSLContextBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.testframework.https.ManagedCertificatesException;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class AdminClientFactory {

    private final Supplier<KeycloakBuilder> delegateSupplier;

    private final List<Keycloak> instanceToClose = new LinkedList<>();

    AdminClientFactory(String serverUrl) {
        delegateSupplier = () -> KeycloakBuilder.builder().serverUrl(serverUrl);
    }

    AdminClientFactory(String serverUrl, KeyStore serverKeyStore) {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(serverKeyStore, null)
                    .build();

            delegateSupplier = () ->
                    KeycloakBuilder.builder()
                            .serverUrl(serverUrl)
                            .resteasyClient(Keycloak.getClientProvider().newRestEasyClient(null, sslContext, false));
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new ManagedCertificatesException(e);
        }
    }

    public AdminClientBuilder create() {
        return new AdminClientBuilder(this, delegateSupplier.get());
    }

    void addToClose(Keycloak keycloak) {
        instanceToClose.add(keycloak);
    }

    public void close() {
        instanceToClose.forEach(Keycloak::close);
    }

}
