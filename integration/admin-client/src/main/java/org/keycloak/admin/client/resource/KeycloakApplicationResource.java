package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.ApplicationService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;

import java.util.Set;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakApplicationResource {

    private final String appName;
    private final ApplicationService applicationService;
    private final Config config;
    private final TokenManager tokenManager;
    private KeycloakApplicationLevelRolesResource applicationLevelRoleService;

    public KeycloakApplicationResource(Config config, TokenManager tokenManager, String appName){
        this.appName = appName;
        this.config = config;
        this.tokenManager = tokenManager;
        this.applicationService = new ApplicationService(config, tokenManager);
    }

    public ApplicationRepresentation getRepresentation(){
        return applicationService.find(appName);
    }

    public Set<String> getAllowedOrigins(){
        return applicationService.getAllowedOrigins(appName);
    }

    public void updateAllowedOrigins(Set<String> newAllowedOrigins){
        applicationService.updateAllowedOrigins(appName, newAllowedOrigins);
    }

    public void removeAllowedOrigins(Set<String> originsToDelete){
        applicationService.removeAllowedOrigins(appName, originsToDelete);
    }

    public ClaimRepresentation getClaims(){
        return applicationService.getClaims(appName);
    }

    public void updateClaims(ClaimRepresentation claimRepresentation){
        applicationService.updateClaims(appName, claimRepresentation);
    }

    public CredentialRepresentation generateNewSecret(){
        return applicationService.generateNewSecret(appName);
    }

    public CredentialRepresentation getSecret(){
        return applicationService.getSecret(appName);
    }

    public String getInstallationJbossXml(){
        return applicationService.getInstallationJbossXml(appName);
    }

    public String getInstallationJson(){
        return applicationService.getInstallationJson(appName);
    }

    public void logoutAllUsers(){
        applicationService.logoutAllUsers(appName);
    }

    public void logoutUser(String username){
        applicationService.logoutUser(appName, username);
    }

    public void pushRevocation(){
        applicationService.pushRevocation(appName);
    }

    public MappingsRepresentation getScopeMappings(){
        return applicationService.getScopeMappings(appName);
    }

    public KeycloakApplicationLevelRolesResource roles(){
        if(applicationLevelRoleService == null){
            applicationLevelRoleService = new KeycloakApplicationLevelRolesResource(config, tokenManager, appName);
        }
        return applicationLevelRoleService;
    }

    public KeycloakRoleResource role(String roleName){
        return new KeycloakRoleResource(config, tokenManager, roleName, appName);
    }

}
