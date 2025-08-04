package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

public class UserRoleRepresentation extends UserRepresentation {
    private Map<String, List<RoleRepresentation>> roles;

    public UserRoleRepresentation(UserRepresentation rep) {
        super(rep);
    }

    public UserRoleRepresentation(UserRepresentation rep, Map<String, List<RoleRepresentation>> roles) {
        super(rep);
        this.roles = roles;
    }

    public Map<String, List<RoleRepresentation>> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, List<RoleRepresentation>> roles) {
        this.roles = roles;
    }
}
