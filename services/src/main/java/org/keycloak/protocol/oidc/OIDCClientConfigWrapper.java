package org.keycloak.protocol.oidc;

import java.util.HashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSecretConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.utils.ClockUtil;
import org.keycloak.utils.StringUtil;

public class OIDCClientConfigWrapper {

  private final ClientModel clientModel;
  private final ClientRepresentation clientRep;

  private OIDCClientConfigWrapper(ClientModel client, ClientRepresentation clientRep) {
    this.clientModel = client;
    this.clientRep = clientRep;
  }

  public static OIDCClientConfigWrapper fromClientModel(ClientModel client) {
    return new OIDCClientConfigWrapper(client, null);
  }

  public static OIDCClientConfigWrapper fromClientRepresentation(ClientRepresentation clientRep) {
    return new OIDCClientConfigWrapper(null, clientRep);
  }

  public String getId() {
    if (clientModel != null) {
      return clientModel.getId();
    } else {
      return clientRep.getId();
    }

  }

  public String getName() {
    if (clientModel != null) {
      return clientModel.getName();
    } else {
      return clientRep.getName();
    }

  }

  public String getDescription() {
    if (clientModel != null) {
      return clientModel.getDescription();
    } else {
      return clientRep.getDescription();
    }
  }

  public String getSecret() {
    if (clientModel != null) {
      return clientModel.getSecret();
    } else {
      return clientRep.getSecret();
    }
  }

  public String getProtocol() {
    if (clientModel != null) {
      return clientModel.getProtocol();
    } else {
      return clientRep.getProtocol();
    }
  }

  public boolean isClientSecretRotationEnabled() {
    String rotationEnabled = getAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATION_ENABLED);
    return rotationEnabled == null ? false : Boolean.valueOf(rotationEnabled); //TODO Optional
  }

  public int getClientSecretCreationTime() {
    String creationTime = getAttribute(ClientSecretConfig.CLIENT_SECRET_CREATION_TIME);
    return creationTime == null ? 0 : Integer.valueOf(creationTime); //TODO Optional
  }

  public void setClientSecretCreationTime(int creationTime) {
    setAttribute(ClientSecretConfig.CLIENT_SECRET_CREATION_TIME, String.valueOf(creationTime));
  }

  public boolean hasRotatedSecret() {
    String secretRotated = getAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED);
    return StringUtil.isNotBlank(secretRotated);
  }

  public String getClientSecretRotated() {
    return getAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED);
  }

  public void setClientSecretRotated(String secret) {
    setAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED, secret);
  }

  public int getClientSecretRotatedCreationTime() {
    return Integer.valueOf(
        getAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED_CREATION_TIME, "0"));
  }

  public void setClientSecretRotatedCreationTime(int rotatedTime) {
    setAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED_CREATION_TIME,
        String.valueOf(rotatedTime));
  }

  /*
  Update the creation time of a secret with current date time value
   */
  public void setClientSecretCreationTime() {
    setClientSecretCreationTime(ClockUtil.currentTimeInSeconds());
  }

  public void setClientSecretRotatedCreationTime() {
    setClientSecretRotatedCreationTime(ClockUtil.currentTimeInSeconds());
  }

  public void updateClientRepresentationAttributes(ClientRepresentation rep) {
    rep.getAttributes().put(ClientSecretConfig.CLIENT_SECRET_CREATION_TIME,
        getAttribute(ClientSecretConfig.CLIENT_SECRET_CREATION_TIME));
    rep.getAttributes().put(ClientSecretConfig.CLIENT_SECRET_EXPIRATION,
        getAttribute(ClientSecretConfig.CLIENT_SECRET_EXPIRATION));
    rep.getAttributes().put(ClientSecretConfig.CLIENT_SECRET_ROTATED_CREATION_TIME,
        getAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED_CREATION_TIME));
    rep.getAttributes().put(ClientSecretConfig.CLIENT_SECRET_ROTATED_EXPIRATION_TIME,
        getAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED_EXPIRATION_TIME));
  }

  private String getAttribute(String attrKey) {
    if (clientModel != null) {
      return clientModel.getAttribute(attrKey);
    } else {
      return clientRep.getAttributes() == null ? null : clientRep.getAttributes().get(attrKey);
    }
  }

  private String getAttribute(String attrKey, String defaultValue) {
    String value = getAttribute(attrKey);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  private void setAttribute(String attrKey, String attrValue) {
    if (clientModel != null) {
      if (attrValue != null) {
        clientModel.setAttribute(attrKey, attrValue);
      } else {
        clientModel.removeAttribute(attrKey);
      }
    } else {
      if (attrValue != null) {
        if (clientRep.getAttributes() == null) {
          clientRep.setAttributes(new HashMap<>());
        }
        clientRep.getAttributes().put(attrKey, attrValue);
      } else {
        if (clientRep.getAttributes() != null) {
          clientRep.getAttributes().put(attrKey, null);
        }
      }
    }
  }

  public void setClientSecretExpirationTime(int expiration) {
    setAttribute(ClientSecretConfig.CLIENT_SECRET_EXPIRATION, String.valueOf(expiration));
  }

  public void setClientSecretRotatedExpirationTime(int expiration) {
    setAttribute(ClientSecretConfig.CLIENT_SECRET_ROTATED_EXPIRATION_TIME,
        String.valueOf(expiration));
  }
}
