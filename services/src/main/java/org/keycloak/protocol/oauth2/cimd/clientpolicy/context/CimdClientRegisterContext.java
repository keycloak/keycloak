package org.keycloak.protocol.oauth2.cimd.clientpolicy.context;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

public class CimdClientRegisterContext implements ClientCRUDContext {

    private final ClientRepresentation proposedClientRepresentation;

    public CimdClientRegisterContext(ClientRepresentation proposedClientRepresentation) {
        this.proposedClientRepresentation = proposedClientRepresentation;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.REGISTER;
    }

    @Override
    public ClientRepresentation getProposedClientRepresentation() {
        return proposedClientRepresentation;
    }
}
