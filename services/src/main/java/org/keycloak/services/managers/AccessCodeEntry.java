package org.keycloak.services.managers;

import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.AccessCode;
import org.keycloak.util.Time;

import java.util.HashSet;
import java.util.Set;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AccessCodeEntry {
    protected AccessCode accessCode;
    protected RealmModel realm;
    KeycloakSession keycloakSession;

    public AccessCodeEntry(KeycloakSession keycloakSession, RealmModel realm, AccessCode accessCode) {
        this.realm = realm;
        this.accessCode = accessCode;
        this.keycloakSession = keycloakSession;
    }

    public String getCodeId() {
        return this.accessCode.getId();
    }

    public UserModel getUser() {
        return keycloakSession.users().getUserById(accessCode.getUserId(), realm);
    }

    public String getSessionState() {
        return accessCode.getSessionState();
    }

    public boolean isExpired() {
        int lifespan = accessCode.getAction() == null ? realm.getAccessCodeLifespan() : realm.getAccessCodeLifespanUserAction();
        return accessCode.getTimestamp() + lifespan < Time.currentTime();
    }

    public Set<RoleModel> getRequestedRoles() {
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();
        for (String roleId : accessCode.getRequestedRoles()) {
            RoleModel role = realm.getRoleById(roleId);
            if (role == null) {
                new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid role " + roleId);
            }
            requestedRoles.add(realm.getRoleById(roleId));
        }
        return requestedRoles;
    }

    public ClientModel getClient() {
        return realm.findClient(accessCode.getClientId());
    }

    public String getState() {
        return accessCode.getState();
    }

    public void setState(String state) {
        accessCode.setState(state);
    }

    public String getRedirectUri() {
        return accessCode.getRedirectUri();
    }

    public AccessCode.Action getAction() {
        return accessCode.getAction();
    }

    public void setAction(AccessCode.Action action) {
        accessCode.setAction(action);
        accessCode.setTimestamp(Time.currentTime());
    }

    public RequiredAction getRequiredAction() {
        AccessCode.Action action = accessCode.getAction();
        if (action != null) {
            switch (action) {
                case CONFIGURE_TOTP:
                    return RequiredAction.CONFIGURE_TOTP;
                case UPDATE_PASSWORD:
                    return RequiredAction.UPDATE_PASSWORD;
                case UPDATE_PROFILE:
                    return RequiredAction.UPDATE_PROFILE;
                case VERIFY_EMAIL:
                    return RequiredAction.VERIFY_EMAIL;
            }
        }
        return null;
    }

    public void setRequiredAction(RequiredAction requiredAction) {
        switch (requiredAction) {
            case CONFIGURE_TOTP:
                setAction(AccessCode.Action.CONFIGURE_TOTP);
                break;
            case UPDATE_PASSWORD:
                setAction(AccessCode.Action.UPDATE_PASSWORD);
                break;
            case UPDATE_PROFILE:
                setAction(AccessCode.Action.UPDATE_PROFILE);
                break;
            case VERIFY_EMAIL:
                setAction(AccessCode.Action.VERIFY_EMAIL);
                break;
            default:
                throw new IllegalArgumentException("Unknown required action " + requiredAction);
        }
    }

    public String getCode() {
        return new JWSBuilder().jsonContent(accessCode).rsa256(realm.getPrivateKey());
    }
}
