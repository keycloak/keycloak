package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.LinkedHashMap;

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
            if (app.getAttributes() == null) {
                app.setAttributes(new LinkedHashMap<String, String>());
            }
            app.getAttributes().put(attribute, value);
            clientResource.update(app);
        }

        public void directAccessGrant(Boolean enable) {
            ClientRepresentation app = clientResource.toRepresentation();
            app.setDirectAccessGrantsEnabled(enable);
            clientResource.update(app);
        }

        public void fullScopeAllowed(boolean enable) {
            ClientRepresentation app = clientResource.toRepresentation();
            app.setFullScopeAllowed(enable);
            clientResource.update(app);
        }

        public void consentRequired(boolean enable) {
            ClientRepresentation app = clientResource.toRepresentation();
            app.setConsentRequired(enable);
            clientResource.update(app);
        }
    }
}
