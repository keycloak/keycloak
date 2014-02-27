package org.keycloak.representations.idm;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeMappingRepresentation {
    protected String self; // link
    protected String client;
    protected Set<String> roles;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public ScopeMappingRepresentation role(String role) {
        if (this.roles == null) this.roles = new HashSet<String>();
        this.roles.add(role);
        return this;
    }

}
