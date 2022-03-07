package org.keycloak.protocol.oidc;

import static org.keycloak.models.ClientSecretConstants.CLIENT_ROTATED_SECRET;
import static org.keycloak.models.ClientSecretConstants.CLIENT_ROTATED_SECRET_CREATION_TIME;
import static org.keycloak.models.ClientSecretConstants.CLIENT_ROTATED_SECRET_EXPIRATION_TIME;
import static org.keycloak.models.ClientSecretConstants.CLIENT_SECRET_CREATION_TIME;
import static org.keycloak.models.ClientSecretConstants.CLIENT_SECRET_EXPIRATION;

import java.security.MessageDigest;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:masales@redhat.com">Marcelo Sales</a>
 */
public class OIDCClientSecretConfigWrapper extends AbstractClientConfigWrapper {

  private OIDCClientSecretConfigWrapper(ClientModel client, ClientRepresentation clientRep) {
    super(client,clientRep);
  }

  public static OIDCClientSecretConfigWrapper fromClientModel(ClientModel client) {
    return new OIDCClientSecretConfigWrapper(client, null);
  }

  public static OIDCClientSecretConfigWrapper fromClientRepresentation(ClientRepresentation clientRep) {
    return new OIDCClientSecretConfigWrapper(null, clientRep);
  }

  public String getSecret() {
    if (clientModel != null) {
      return clientModel.getSecret();
    } else {
      return clientRep.getSecret();
    }
  }

  public void removeClientSecretRotated() {
    if (hasRotatedSecret()) {
      setAttribute(CLIENT_ROTATED_SECRET, null);
      setAttribute(CLIENT_ROTATED_SECRET_CREATION_TIME, null);
      setAttribute(CLIENT_ROTATED_SECRET_EXPIRATION_TIME,null);
    }
  }

  public int getClientSecretCreationTime() {
    String creationTime = getAttribute(CLIENT_SECRET_CREATION_TIME);
    return StringUtil.isBlank(creationTime) ? 0 : Integer.parseInt(creationTime);
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

  public boolean hasClientSecretExpirationTime() {
    return getClientSecretExpirationTime() > 0;
  }

  public int getClientSecretExpirationTime() {
    String expiration = getAttribute(CLIENT_SECRET_EXPIRATION);
    return expiration == null ? 0 : Integer.parseInt(expiration);
  }

  public void setClientSecretExpirationTime(Integer expiration) {
    setAttribute(ClientSecretConstants.CLIENT_SECRET_EXPIRATION, String.valueOf(expiration));
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
          getAttribute(ClientSecretConstants.CLIENT_ROTATED_SECRET_EXPIRATION_TIME));
    }
    return 0;
  }

  public void setClientRotatedSecretExpirationTime(Integer expiration) {
    setAttribute(ClientSecretConstants.CLIENT_ROTATED_SECRET_EXPIRATION_TIME,
        String.valueOf(expiration));
  }

  public boolean hasClientRotatedSecretExpirationTime() {
    return StringUtil.isNotBlank(
        getAttribute(ClientSecretConstants.CLIENT_ROTATED_SECRET_EXPIRATION_TIME));
  }

  public boolean isClientRotatedSecretExpired() {
    if (hasClientRotatedSecretExpirationTime()) {
      return getClientRotatedSecretExpirationTime() < Time.currentTime();
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
