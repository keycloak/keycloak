package org.keycloak.services.managers;

import org.keycloak.representations.SkeletonKeyToken;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.User;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
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
    protected User user;
    protected User client;
    protected List<Role> realmRolesRequested = new ArrayList<Role>();
    MultivaluedMap<String, Role> resourceRolesRequested = new MultivaluedHashMap<String, Role>();

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

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Role> getRealmRolesRequested() {
        return realmRolesRequested;
    }

    public MultivaluedMap<String, Role> getResourceRolesRequested() {
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
