package org.keycloak.testframework.admin;

import jakarta.ws.rs.client.Client;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.testframework.https.ManagedCertificateException;

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

    AdminClientFactory(String serverUrl, KeyStore serverTrustStore) {
        try {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(serverTrustStore, new TrustSelfSignedStrategy())
                .build();

            Client restEasyClient = Keycloak.getClientProvider().newRestEasyClient(null, sslContext, false);
            delegateSupplier = () -> KeycloakBuilder.builder().serverUrl(serverUrl).resteasyClient(restEasyClient);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new ManagedCertificateException(e);
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
