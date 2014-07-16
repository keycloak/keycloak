package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.ApplicationService;
import org.keycloak.admin.client.service.RoleService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakRoleResource {

    private final String roleId;
    private final RoleService roleService;

    /**
     * This constructor is used when treating an application level role
     */
    public KeycloakRoleResource(Config config, TokenManager tokenManager, String roleName, String appName){
        roleService = new RoleService(config, tokenManager);
        roleId = new ApplicationService(config, tokenManager).getRole(appName, roleName).getId();
    }

    /**
     * This constructor is used when treating a realm level role
     */
    public KeycloakRoleResource(Config config, TokenManager tokenManager, String roleName){
        roleService = new RoleService(config, tokenManager);
        roleId = roleService.findRealmRoleByName(roleName).getId();
    }

    public RoleRepresentation getRepresentation(){
        return roleService.findById(roleId);
    }

    public Set<RoleRepresentation> getChildren(){
        return roleService.getChildrenById(roleId);
    }

    public void addChild(RoleRepresentation roleToAdd){
        addChildren(Arrays.asList(roleToAdd));
    }

    public void addChildren(List<RoleRepresentation> rolesToAdd){
        roleService.addChildrenById(roleId, rolesToAdd);
    }

    public void removeChild(RoleRepresentation roleToRemove){
        removeChildren(Arrays.asList(roleToRemove));
    }

    public void removeChildren(List<RoleRepresentation> rolesToRemove){
        roleService.removeChildrenById(roleId, rolesToRemove);
    }

    public Set<RoleRepresentation> getApplicationLevelChildren(String appName){
        return roleService.getApplicationLevelRoleById(roleId, appName);
    }

    public Set<RoleRepresentation> getRealmLevelChildren(){
        return roleService.getRealmLevelChildrenById(roleId);
    }

}
