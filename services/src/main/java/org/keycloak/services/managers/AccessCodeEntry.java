package org.keycloak.services.managers;

import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserModel.RequiredAction;

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

    protected long expiration;
    protected SkeletonKeyToken token;
    protected UserModel user;
    protected Set<RequiredAction> requiredActions;
    protected UserModel client;
    protected List<RoleModel> realmRolesRequested = new ArrayList<RoleModel>();
    MultivaluedMap<String, RoleModel> resourceRolesRequested = new MultivaluedHashMap<String, RoleModel>();

    public boolean isExpired() {
        return expiration != 0 && (System.currentTimeMillis() / 1000) > expiration;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public SkeletonKeyToken getToken() {
        return token;
    }

    public void setToken(SkeletonKeyToken token) {
        this.token = token;
    }

    public UserModel getClient() {
        return client;
    }

    public void setClient(UserModel client) {
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
}
