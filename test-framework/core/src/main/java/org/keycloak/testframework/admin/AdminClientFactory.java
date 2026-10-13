package org.keycloak.testframework.admin;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class AdminClientFactory {

    private final Supplier<KeycloakBuilder> delegateSupplier;

    private final List<Keycloak> instanceToClose = new LinkedList<>();

    AdminClientFactory(String serverUrl) {
        delegateSupplier = () -> KeycloakBuilder.builder().serverUrl(serverUrl);
    }

    AdminClientFactory(String serverUrl, SSLContext sslContext) {
            delegateSupplier = () ->
                    KeycloakBuilder.builder()
                            .serverUrl(serverUrl)
                            .resteasyClient(Keycloak.getClientProvider().newRestEasyClient(null, sslContext, false));
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
