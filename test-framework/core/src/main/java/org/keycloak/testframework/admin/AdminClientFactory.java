package org.keycloak.testframework.admin;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class AdminClientFactory {

    private final Supplier<KeycloakBuilder> delegateSupplier;

    private final List<Keycloak> instanceToClose = new LinkedList<>();

    AdminClientFactory(String serverUrl) {
        delegateSupplier = () -> KeycloakBuilder.builder().serverUrl(serverUrl);
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
