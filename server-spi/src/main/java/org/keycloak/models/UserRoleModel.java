package org.keycloak.models;

import java.util.List;
import java.util.Map;

public class UserRoleModel {
    private UserModel user;

    public UserModel getUser() {
        return user;
    }

    private Map<String, List<RoleModel>> roles;

    public Map<String, List<RoleModel>> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, List<RoleModel>> roles) {
        this.roles = roles;
    }

    public UserRoleModel(UserModel user) {
        this.user = user;
    }
}
