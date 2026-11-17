package org.keycloak.protocol.oauth2.cimd.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientCRUDClientAvailableContext;

public class CimdClientRegisteredContext implements ClientCRUDClientAvailableContext {

    private final ClientModel registeredClient;

    public CimdClientRegisteredContext(ClientModel registeredClient) {
        this.registeredClient = registeredClient;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.REGISTERED;
    }

    @Override
    public ClientModel getTargetClient() {
        return registeredClient;
    }
}
