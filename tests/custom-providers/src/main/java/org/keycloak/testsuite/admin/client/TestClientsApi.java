package org.keycloak.testsuite.admin.client;

import jakarta.enterprise.context.RequestScoped;
import org.keycloak.admin.api.ChosenBySpi;
import org.keycloak.admin.api.FieldValidation;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.api.client.ClientsApi;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import java.util.stream.Stream;

@RequestScoped
@ChosenBySpi
public class TestClientsApi implements ClientsApi {

    @Override
    public Stream<ClientRepresentation> getClients() {
        var client = new ClientRepresentation();
        client.setClientId("test");
        client.setDisplayName("testCdi");
        return Stream.of(client);
    }

    @Override
    public ClientRepresentation createClient(ClientRepresentation client, FieldValidation fieldValidation) {
        throw new UnsupportedOperationException("Testing instance");
    }

    @Override
    public ClientApi client(String id) {
        throw new UnsupportedOperationException("Testing instance");
    }

    @Override
    public void close() {

    }
}
