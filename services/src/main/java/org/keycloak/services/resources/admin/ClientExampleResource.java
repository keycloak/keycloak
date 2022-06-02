package org.keycloak.services.resources.admin;

import java.util.ArrayList;
import java.util.List;

import org.openapitools.model.Client;
import org.openapitools.model.PartialClient;

/**
 * The interface is generated and we "only" need to implement them.
 * PartialClient is to have type save responses instead of passing in `briefRepresentation` and only partially filling the entity
 */
public class ClientExampleResource implements ClientsApi {
    @Override
    public void createClient(Client client) {

    }

    @Override
    public void deleteClient(String clientId) {

    }

    @Override
    public Client getClient(String clientId) {
        return null;
    }

    @Override
    public List<PartialClient> getClients() {
        List<PartialClient> partialClients = new ArrayList<>(2);
        final PartialClient client = new PartialClient();
        client.id("71e7f5d7-a093-4ddc-8847-3f45be0a662b");
        client.setClientId("account");
        partialClients.add(client);
        final PartialClient client2 = new PartialClient();
        client2.id("4389e5be-22b7-44d9-9c9c-ee4bc6da0159");
        client2.setClientId("security-admin-console");
        partialClients.add(client2);

        return partialClients;
    }

    @Override
    public void updateClient(String clientId, Client client) {

    }
}
