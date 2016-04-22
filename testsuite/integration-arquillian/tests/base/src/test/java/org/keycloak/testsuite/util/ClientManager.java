package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.Map;

import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class ClientManager {

    private static RealmResource realm;

    private ClientManager() {
    }

    public static ClientManager realm(RealmResource realm) {
        ClientManager.realm = realm;
        return new ClientManager();
    }

    public ClientManagerBuilder clientId(String clientId) {
        return new ClientManagerBuilder(findClientByClientId(realm, clientId));
    }

    public class ClientManagerBuilder {

        private final ClientResource clientResource;

        public ClientManagerBuilder(ClientResource clientResource) {
            this.clientResource = clientResource;
        }

        public void renameTo(String newName) {
            ClientRepresentation app = clientResource.toRepresentation();
            app.setClientId(newName);
            clientResource.update(app);
        }

        public void enabled(Boolean enabled) {
            ClientRepresentation app = clientResource.toRepresentation();
            app.setEnabled(enabled);
            clientResource.update(app);
        }

        public void updateAttribute(String attribute, String value) {
            ClientRepresentation app = clientResource.toRepresentation();
            app.getAttributes().put(attribute, value);
            clientResource.update(app);
        }

    }
}
