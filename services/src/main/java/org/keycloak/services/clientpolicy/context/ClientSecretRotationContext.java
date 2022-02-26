package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.resources.admin.AdminAuth;

public class ClientSecretRotationContext extends AdminClientUpdateContext {

  private final String currentSecret;
  private final boolean forceRotation;

  public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
      ClientModel targetClient, AdminAuth adminAuth, String currentSecret,boolean forceRotation) {
    super(proposedClientRepresentation, targetClient, adminAuth);
    this.currentSecret= currentSecret;
    this.forceRotation = forceRotation;
  }

  public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
      ClientModel targetClient, AdminAuth adminAuth, String currentSecret) {
    super(proposedClientRepresentation, targetClient, adminAuth);
    this.currentSecret= currentSecret;
    this.forceRotation = false;
  }

  public ClientSecretRotationContext(ClientRepresentation proposedClientRepresentation,
      ClientModel targetClient, AdminAuth adminAuth) {
    super(proposedClientRepresentation, targetClient, adminAuth);
    this.currentSecret= null;
    this.forceRotation = false;
  }

  public String getCurrentSecret() {
    return currentSecret;
  }

  public boolean isForceRotation() {
    return forceRotation;
  }
}
