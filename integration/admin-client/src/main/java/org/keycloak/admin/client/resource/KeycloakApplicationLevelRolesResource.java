package org.keycloak.admin.client.resource;


import org.keycloak.admin.client.service.interfaces.ApplicationRolesService;
import org.keycloak.admin.client.service.interfaces.ApplicationService;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakApplicationLevelRolesResource {

    private final ApplicationRolesService appRolesService;
    
    public KeycloakApplicationLevelRolesResource(ApplicationService applicationService){
        appRolesService = applicationService.roles();
    }

    public List<RoleRepresentation> getList(){
        return appRolesService.list();
    }

    public RoleRepresentation get(String roleName){
        return appRolesService.getRepresentation(roleName);
    }

    public void create(RoleRepresentation roleRepresentation){
        appRolesService.create(roleRepresentation);
    }

    public void update(RoleRepresentation roleRepresentation){
        appRolesService.update(roleRepresentation.getName(), roleRepresentation);
    }

    public void remove(String roleName){
        appRolesService.remove(roleName);
    }

}
