package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.utils.StringUtil;

public class ClientSecretRotationContext extends AdminClientUpdateContext {

  private final String currentSecret;

  public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
      ClientModel targetClient, AdminAuth adminAuth, String currentSecret) {
    super(proposedClientRepresentation, targetClient, adminAuth);
    this.currentSecret= currentSecret;
  }

  public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
      ClientModel targetClient, String currentSecret) {
    super(proposedClientRepresentation, targetClient, null);
    this.currentSecret= currentSecret;
  }

  public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
      ClientModel targetClient) {
    super(proposedClientRepresentation, targetClient, null);
    this.currentSecret= null;
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
