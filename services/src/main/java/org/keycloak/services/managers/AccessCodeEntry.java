package org.keycloak.services.managers;

import org.keycloak.representations.SkeletonKeyToken;
import org.picketlink.idm.model.User;

import java.util.UUID;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AccessCodeEntry {
    protected String id = UUID.randomUUID().toString() + System.currentTimeMillis();
    protected long expiration;
    protected SkeletonKeyToken token;
    protected User client;

    public boolean isExpired() {
        return expiration != 0 && (System.currentTimeMillis() / 1000) > expiration;
    }

    public String getId() {
        return id;
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
}
