package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.interfaces.AdminRootFactory;
import org.keycloak.admin.client.service.interfaces.ApplicationsService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.ApplicationRepresentation;

import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakApplicationsResource {

    private ApplicationsService applicationsService;

    public KeycloakApplicationsResource(Config config, TokenManager tokenManager){
        applicationsService = AdminRootFactory.getAdminRoot(config, tokenManager).realm(config.getRealm()).applications();
    }

    public List<ApplicationRepresentation> findAll(){
        return applicationsService.findAll();
    }

    public void create(ApplicationRepresentation applicationRepresentation){
        applicationsService.create(applicationRepresentation);
    }

    public ApplicationRepresentation find(String appName){
        return applicationsService.get(appName).getRepresentation();
    }

    public void update(ApplicationRepresentation applicationRepresentation){
        String appName = applicationRepresentation.getName();
        applicationsService.get(appName).update(applicationRepresentation);
    }

    public void remove(String appName){
        applicationsService.get(appName).remove();
    }

}
