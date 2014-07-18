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
import org.keycloak.representations.AccessToken;
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

    public void setSessionState(String state) {
        accessCode.setSessionState(state);
    }

    public boolean isExpired() {
        return accessCode.getExpiration() != 0 && Time.currentTime() > accessCode.getExpiration();
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

    public boolean isRememberMe() {
        return accessCode.isRememberMe();
    }

    public void setRememberMe(boolean remember) {
        accessCode.setRememberMe(remember);
    }

    public String getAuthMethod() {
        return accessCode.getAuthMethod();
    }

    public String getUsernameUsed() {
        return accessCode.getUsernameUsed();
    }

    public void setUsernameUsed(String username) {
        accessCode.setUsernameUsed(username);
    }

    public void resetExpiration() {
        accessCode.setExpiration(Time.currentTime() + realm.getAccessCodeLifespan());

    }

    public void setAuthMethod(String authMethod) {
        accessCode.setAuthMethod(authMethod);
    }

    public Set<RequiredAction> getRequiredActions() {
        Set<RequiredAction> set = new HashSet<RequiredAction>();
        for (String action : accessCode.getRequiredActions()) {
            set.add(RequiredAction.valueOf(action));

        }
        return set;
    }

    public boolean hasRequiredAction(RequiredAction action) {
        return accessCode.getRequiredActions().contains(action.toString());
    }

    public void removeRequiredAction(RequiredAction action) {
        accessCode.getRequiredActions().remove(action.toString());
    }

    public void setRequiredActions(Set<RequiredAction> set) {
        Set<String> newSet = new HashSet<String>();
        for (RequiredAction action : set) {
            newSet.add(action.toString());
        }
        accessCode.setRequiredActions(newSet);
    }

    public String getCode() {
       return new JWSBuilder().jsonContent(accessCode).rsa256(realm.getPrivateKey());
    }
}
