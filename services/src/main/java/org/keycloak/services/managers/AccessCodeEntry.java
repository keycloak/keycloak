package org.keycloak.services.managers;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.Time;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AccessCodeEntry {
    protected String id = UUID.randomUUID().toString() + System.currentTimeMillis();
    protected String code;
    protected String state;
    protected String redirectUri;
    protected boolean rememberMe;

    protected int expiration;
    protected RealmModel realm;
    protected AccessToken token;
    protected UserModel user;
    protected Set<RequiredAction> requiredActions;
    protected ClientModel client;
    protected List<RoleModel> realmRolesRequested = new ArrayList<RoleModel>();
    MultivaluedMap<String, RoleModel> resourceRolesRequested = new MultivaluedHashMap<String, RoleModel>();

    public boolean isExpired() {
        return expiration != 0 && Time.currentTime() > expiration;
    }

    public String getId() {
        return id;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public void setRealm(RealmModel realm) {
        this.realm = realm;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getExpiration() {
        return expiration;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }

    public AccessToken getToken() {
        return token;
    }

    public void setToken(AccessToken token) {
        this.token = token;
    }

    public ClientModel getClient() {
        return client;
    }

    public void setClient(ClientModel client) {
        this.client = client;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public Set<RequiredAction> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(Set<RequiredAction> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public List<RoleModel> getRealmRolesRequested() {
        return realmRolesRequested;
    }

    public MultivaluedMap<String, RoleModel> getResourceRolesRequested() {
        return resourceRolesRequested;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
