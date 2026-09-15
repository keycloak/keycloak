package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.utils.StringUtil;

public class ClientSecretRotationContext extends AdminClientUpdatedContext {

    private final String currentSecret;

    public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
                                       ClientModel targetClient, String currentSecret, AdminAuth adminAuth) {
        super(proposedClientRepresentation, targetClient, adminAuth);
        this.currentSecret = currentSecret;
    }

    public String getCurrentSecret() {
        return currentSecret;
    }

    public boolean isForceRotation() {
        return StringUtil.isNotBlank(currentSecret);
    }
}
