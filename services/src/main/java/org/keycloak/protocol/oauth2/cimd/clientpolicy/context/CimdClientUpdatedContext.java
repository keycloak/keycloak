package org.keycloak.protocol.oauth2.cimd.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientCRUDClientAvailableContext;

public class CimdClientUpdatedContext implements ClientCRUDClientAvailableContext {

    private final ClientModel updatedClient;

    public CimdClientUpdatedContext(ClientModel updatedClient) {
        this.updatedClient = updatedClient;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.UPDATED;
    }

    @Override
    public ClientModel getTargetClient() {
        return updatedClient;
    }
}
