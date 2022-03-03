package org.keycloak.protocol.oidc;

import static org.keycloak.models.ClientSecretConfig.CLIENT_ROTATED_SECRET;
import static org.keycloak.models.ClientSecretConfig.CLIENT_ROTATED_SECRET_CREATION_TIME;
import static org.keycloak.models.ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRATION_TIME;
import static org.keycloak.models.ClientSecretConfig.CLIENT_SECRET_CREATION_TIME;
import static org.keycloak.models.ClientSecretConfig.CLIENT_SECRET_EXPIRATION;
import static org.keycloak.models.ClientSecretConfig.CLIENT_SECRET_ROTATION_ENABLED;

import java.security.MessageDigest;
import java.util.HashMap;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSecretConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
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

  public void removeClientSecretRotated() {
    if (hasRotatedSecret()) {
      setAttribute(CLIENT_ROTATED_SECRET, null);
      setAttribute(CLIENT_ROTATED_SECRET_CREATION_TIME, null);
    }
  }

  public int getClientSecretCreationTime() {
    String creationTime = getAttribute(CLIENT_SECRET_CREATION_TIME);
    return creationTime == null ? 0 : Integer.parseInt(creationTime);
  }

  public void setClientSecretCreationTime(int creationTime) {
    setAttribute(CLIENT_SECRET_CREATION_TIME, String.valueOf(creationTime));
  }

  public boolean hasRotatedSecret() {
    return StringUtil.isNotBlank(getAttribute(CLIENT_ROTATED_SECRET)) && StringUtil.isNotBlank(
        getAttribute(CLIENT_ROTATED_SECRET_CREATION_TIME));
  }

  public String getClientRotatedSecret() {
    return getAttribute(CLIENT_ROTATED_SECRET);
  }

  public void setClientRotatedSecret(String secret) {
    setAttribute(CLIENT_ROTATED_SECRET, secret);
  }

  public int getClientRotatedSecretCreationTime() {
    return Integer.parseInt(getAttribute(CLIENT_ROTATED_SECRET_CREATION_TIME, "0"));
  }

  public void setClientRotatedSecretCreationTime(Integer rotatedTime) {
    setAttribute(CLIENT_ROTATED_SECRET_CREATION_TIME, String.valueOf(rotatedTime));
  }

  /*
  Update the creation time of a secret with current date time value
   */
  public void setClientSecretCreationTime() {
    setClientSecretCreationTime(Time.currentTime());
  }

  public void setClientRotatedSecretCreationTime() {
    setClientRotatedSecretCreationTime(Time.currentTime());
  }

  public void updateClientRepresentationAttributes(ClientRepresentation rep) {
    rep.getAttributes().put(CLIENT_ROTATED_SECRET, getAttribute(CLIENT_ROTATED_SECRET));
    rep.getAttributes().put(CLIENT_SECRET_CREATION_TIME, getAttribute(CLIENT_SECRET_CREATION_TIME));
    rep.getAttributes().put(CLIENT_SECRET_EXPIRATION, getAttribute(CLIENT_SECRET_EXPIRATION));
    rep.getAttributes().put(CLIENT_ROTATED_SECRET_CREATION_TIME,
        getAttribute(CLIENT_ROTATED_SECRET_CREATION_TIME));
    rep.getAttributes().put(CLIENT_ROTATED_SECRET_EXPIRATION_TIME,
        getAttribute(CLIENT_ROTATED_SECRET_EXPIRATION_TIME));
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

  public boolean hasClientSecretExpirationTime() {
    return getClientSecretExpirationTime() > 0;
  }

  public int getClientSecretExpirationTime() {
    String expiration = getAttribute(CLIENT_SECRET_EXPIRATION);
    return expiration == null ? 0 : Integer.parseInt(expiration);
  }

  public void setClientSecretExpirationTime(Integer expiration) {
    setAttribute(ClientSecretConfig.CLIENT_SECRET_EXPIRATION, String.valueOf(expiration));
  }

  public boolean isClientSecretExpired() {
    if (hasClientSecretExpirationTime()) {
      if (getClientSecretExpirationTime() < Time.currentTime()) {
        return true;
      }
    }
    return false;
  }

  public int getClientRotatedSecretExpirationTime() {
    if (hasClientRotatedSecretExpirationTime()) {
      return Integer.valueOf(
          getAttribute(ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRATION_TIME));
    }
    return 0;
  }

  public void setClientRotatedSecretExpirationTime(Integer expiration) {
    setAttribute(ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRATION_TIME,
        String.valueOf(expiration));
  }

  public boolean hasClientRotatedSecretExpirationTime() {
    return StringUtil.isNotBlank(
        getAttribute(ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRATION_TIME));
  }

  public boolean isClientRotatedSecretExpired() {
    if (hasClientRotatedSecretExpirationTime()) {
      return getClientRotatedSecretExpirationTime() < Time.currentTime();
    }
    return true;
  }

  // validates the secret regarding to expiration (not value itself)
  public boolean validateClientSecret() {
      if (isClientSecretExpired()) {
        return false;
      }
    return true;
  }

  //validates the rotated secret (value and expiration)
  public boolean validateRotatedSecret(String secret) {

    // there must exist a rotated_secret
    if (hasRotatedSecret()) {
      // the rotated secret must not be outdated
      if (isClientRotatedSecretExpired()) {
        return false;
      }
    } else {
      return false;
    }

    return MessageDigest.isEqual(secret.getBytes(), getClientRotatedSecret().getBytes());

  }

}
