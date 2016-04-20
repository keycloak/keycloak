package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.List;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class ClientManager {

    private static RealmResource realm;

    private ClientManager(){}

    public static ClientManager realm(RealmResource realm) {
        ClientManager.realm = realm;
        return new ClientManager();
    }

    public void rename(String oldName, String newName) {
        List<ClientRepresentation> client = realm.clients().findByClientId(oldName);
        if (!client.isEmpty()) {
            ClientResource clientResource = realm.clients().get(client.get(0).getId());
            ClientRepresentation app = clientResource.toRepresentation();
            app.setClientId(newName);
            clientResource.update(app);
        }
    }
}
