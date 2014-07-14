package org.keycloak.admin.client.resource;


import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.ApplicationService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakApplicationLevelRolesResource {

    private final String appName;
    private final ApplicationService applicationService;
    
    public KeycloakApplicationLevelRolesResource(Config config, TokenManager tokenManager, String appName){
        this.appName = appName;
        this.applicationService = new ApplicationService(config, tokenManager);
    }

    public List<RoleRepresentation> getList(){
        return applicationService.getRoles(appName);
    }

    public RoleRepresentation get(String roleName){
        return applicationService.getRole(appName, roleName);
    }

    public void create(RoleRepresentation roleRepresentation){
        applicationService.createRole(appName, roleRepresentation);
    }

    public void update(RoleRepresentation roleRepresentation){
        applicationService.updateRole(appName, roleRepresentation);
    }

    public void remove(String roleName){
        applicationService.removeRole(appName, roleName);
    }

}
