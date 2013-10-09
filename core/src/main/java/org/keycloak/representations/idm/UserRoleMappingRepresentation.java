package org.keycloak.representations.idm;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserRoleMappingRepresentation {
    protected String self; // link
    protected String username;
    protected Set<String> roles;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public UserRoleMappingRepresentation role(String role) {
        if (this.roles == null) this.roles = new HashSet<String>();
        this.roles.add(role);
        return this;
    }

}
