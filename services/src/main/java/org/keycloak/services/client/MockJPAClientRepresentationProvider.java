package org.keycloak.services.client;

import org.keycloak.representations.admin.v2.ClientRepresentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

// Just a simplification to get the abstractions right.
// Later, this will be converted to proper JPA entities and repositories.
public class MockJPAClientRepresentationProvider {

    private Map<String, ClientRepresentation> clients = new LinkedHashMap<>();

    public void addClient(ClientRepresentation client) {
        clients.put(client.getClientId(), client);
    }

    public ClientRepresentation getClient(String clientId) {
        return clients.get(clientId);
    }

    public Stream<ClientRepresentation> stream() {
        return clients.values().stream();
    }
}
