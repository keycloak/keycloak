package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.ApplicationService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.ApplicationRepresentation;

import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakApplicationsResource {

    private final ApplicationService applicationService;

    public KeycloakApplicationsResource(Config config, TokenManager tokenManager){
        applicationService = new ApplicationService(config, tokenManager);
    }

    public List<ApplicationRepresentation> findAll(){
        return applicationService.findAll();
    }

    public void create(ApplicationRepresentation applicationRepresentation){
        applicationService.create(applicationRepresentation);
    }

    public ApplicationRepresentation find(String appName){
        return applicationService.find(appName);
    }

    public void update(ApplicationRepresentation applicationRepresentation){
        applicationService.update(applicationRepresentation);
    }

    public void remove(String appName){
        applicationService.remove(appName);
    }

}
