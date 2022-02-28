package org.keycloak.protocol.oidc;

import static org.keycloak.models.ClientSecretConfig.CLIENT_ROTATED_SECRET;
import static org.keycloak.models.ClientSecretConfig.CLIENT_ROTATED_SECRET_CREATION_TIME;
import static org.keycloak.models.ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRATION_TIME;
import static org.keycloak.models.ClientSecretConfig.CLIENT_SECRET_CREATION_TIME;
import static org.keycloak.models.ClientSecretConfig.CLIENT_SECRET_EXPIRATION;
import static org.keycloak.models.ClientSecretConfig.CLIENT_SECRET_ROTATION_ENABLED;

import java.security.MessageDigest;
import java.util.HashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSecretConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.utils.ClockUtil;
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

  public boolean isClientSecretRotationEnabled() {
    return Boolean.parseBoolean(getAttribute(CLIENT_SECRET_ROTATION_ENABLED));
  }

  public int getClientSecretCreationTime() {
    String creationTime = getAttribute(CLIENT_SECRET_CREATION_TIME);
    return creationTime == null ? 0 : Integer.parseInt(creationTime); //TODO Optional
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
    setClientSecretCreationTime(ClockUtil.currentTimeInSeconds());
  }

  public void setClientRotatedSecretCreationTime() {
    setClientRotatedSecretCreationTime(ClockUtil.currentTimeInSeconds());
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

  public void setClientSecretExpirationTime(int expiration) {
    setAttribute(ClientSecretConfig.CLIENT_SECRET_EXPIRATION, String.valueOf(expiration));
  }

  public void setClientRotatedSecretExpirationTime(int expiration) {
    setAttribute(ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRATION_TIME,
        String.valueOf(expiration));
  }

  public boolean hasClientSecretExpired() {
    return StringUtil.isNotBlank(getAttribute(ClientSecretConfig.CLIENT_SECRET_EXPIRED));
  }

  public boolean isClientSecretExpired() {
    if (hasClientSecretExpired()) {
      return Boolean.parseBoolean(getAttribute(ClientSecretConfig.CLIENT_SECRET_EXPIRED));
    }
    //FIXME identify a scenario where the policy is not executed before credential validation
    throw new IllegalStateException(
        "attribute " + ClientSecretConfig.CLIENT_SECRET_EXPIRED + " not found");
  }

  public boolean hasClientRotatedSecretExpired() {
    return StringUtil.isNotBlank(getAttribute(ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRED));
  }

  public boolean isClientRotatedSecretExpired() {
    if (hasClientRotatedSecretExpired()) {
      return Boolean.parseBoolean(getAttribute(ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRED));
    }
    //FIXME identify a scenario where the policy is not executed before credential validation
    throw new IllegalStateException(
        "attribute " + ClientSecretConfig.CLIENT_ROTATED_SECRET_EXPIRED + " not found");
  }

  // validates the secret regarding to expiration (not value itself)
  public boolean validateClientSecret() {
    if (isClientSecretRotationEnabled()) {
      if (isClientSecretExpired()) {
        return false;
      }
    }

    return true;
  }

  //validates the rotated secret (value and expiration)
  public boolean validateRotatedSecret(String secret) {
    //rotation must be enabled for the client
    if (isClientSecretRotationEnabled()) {
      // there must exist a rotated_secret
      if (hasRotatedSecret()) {
        // the rotated secret must not be outdated
        if (isClientRotatedSecretExpired()) {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }

    return MessageDigest.isEqual(secret.getBytes(), getClientRotatedSecret().getBytes());

  }

}
