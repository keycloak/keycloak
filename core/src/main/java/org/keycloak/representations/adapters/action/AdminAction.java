package org.keycloak.representations.adapters.action;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.keycloak.util.Time;

/**
 * Posted to managed client from admin server.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AdminAction {
    protected String id;
    protected int expiration;
    protected String resource;
    protected String action;

    public AdminAction() {
    }

    public AdminAction(String id, int expiration, String resource, String action) {
        this.id = id;
        this.expiration = expiration;
        this.resource = resource;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public boolean isExpired() {
        return Time.currentTime() > expiration;
    }

    /**
     * Time in seconds since epoc
     *
     * @return
     */
    public int getExpiration() {
        return expiration;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public abstract boolean validate();
}
