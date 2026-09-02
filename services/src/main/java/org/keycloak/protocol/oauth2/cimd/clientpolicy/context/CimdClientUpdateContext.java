package org.keycloak.protocol.oauth2.cimd.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientCRUDClientAvailableContext;

public class CimdClientUpdateContext implements ClientCRUDClientAvailableContext {

    private final ClientRepresentation proposedClientRepresentation;
    private final ClientModel targetClient;

    public CimdClientUpdateContext(ClientRepresentation proposedClientRepresentation, ClientModel targetClient) {
        this.proposedClientRepresentation = proposedClientRepresentation;
        this.targetClient = targetClient;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.UPDATE;
    }

    @Override
    public ClientRepresentation getProposedClientRepresentation() {
        return proposedClientRepresentation;
    }

    @Override
    public ClientModel getTargetClient() {
        return targetClient;
    }
}
