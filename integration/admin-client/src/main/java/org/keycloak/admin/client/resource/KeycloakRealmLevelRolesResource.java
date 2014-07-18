package org.keycloak.admin.client.resource;


import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.RoleService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakRealmLevelRolesResource {

    private final RoleService roleService;

    public KeycloakRealmLevelRolesResource(Config config, TokenManager tokenManager){
        roleService = new RoleService(config, tokenManager);
    }

    public List<RoleRepresentation> getList(){
        return roleService.findAllRealmRoles();
    }

    public RoleRepresentation get(String roleName){
        return roleService.findRealmRoleByName(roleName);
    }

    public void create(RoleRepresentation roleRepresentation){
        roleService.createRole(roleRepresentation);
    }

    public void update(RoleRepresentation roleRepresentation){
        roleService.updateRole(roleRepresentation);
    }

    public void remove(String roleName){
        roleService.removeRole(roleName);
    }

}
