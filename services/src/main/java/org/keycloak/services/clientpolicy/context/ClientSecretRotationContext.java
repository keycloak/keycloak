package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.utils.StringUtil;

public class ClientSecretRotationContext extends AdminClientUpdateContext {

    private final String currentSecret;

    public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
                                       ClientModel targetClient, String currentSecret) {
        super(proposedClientRepresentation, targetClient, null);
        this.currentSecret = currentSecret;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.UPDATED;
    }

    public String getCurrentSecret() {
        return currentSecret;
    }

    public boolean isForceRotation() {
        return StringUtil.isNotBlank(currentSecret);
    }
}
