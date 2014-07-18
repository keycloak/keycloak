package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.interfaces.AdminRootFactory;
import org.keycloak.admin.client.service.interfaces.ApplicationService;
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

    private ApplicationService applicationService;
    private KeycloakApplicationLevelRolesResource applicationLevelRoleService;

    public KeycloakApplicationResource(Config config, TokenManager tokenManager, String appName){
        applicationService = AdminRootFactory.getAdminRoot(config, tokenManager).realm(config.getRealm()).applications().get(appName);
    }

    public ApplicationRepresentation getRepresentation(){
        return applicationService.getRepresentation();
    }

    public Set<String> getAllowedOrigins(){
        return applicationService.getAllowedOrigins();
    }

    public void updateAllowedOrigins(Set<String> newAllowedOrigins){
        applicationService.updateAllowedOrigins(newAllowedOrigins);
    }

    public void removeAllowedOrigins(Set<String> originsToRemove){
        applicationService.removeAllowedOrigins(originsToRemove);
    }

    public ClaimRepresentation getClaims(){
        return applicationService.getClaims();
    }

    public void updateClaims(ClaimRepresentation claimRepresentation){
        applicationService.updateClaims(claimRepresentation);
    }

    public CredentialRepresentation generateNewSecret(){
        return applicationService.generateNewSecret();
    }

    public CredentialRepresentation getSecret(){
        return applicationService.getSecret();
    }

    public String getInstallationJbossXml(){
        return applicationService.getInstallationJbossXml();
    }

    public String getInstallationJson(){
        return applicationService.getInstallationJson();
    }

    public void logoutAllUsers(){
        applicationService.logoutAllUsers();
    }

    public void logoutUser(String username){
        applicationService.logoutUser(username);
    }

    public void pushRevocation(){
        applicationService.pushRevocation();
    }

    public MappingsRepresentation getScopeMappings(){
        return applicationService.getScopeMappings();
    }

    public KeycloakApplicationLevelRolesResource roles(){
        if(applicationLevelRoleService == null){
            applicationLevelRoleService = new KeycloakApplicationLevelRolesResource(applicationService);
        }
        return applicationLevelRoleService;
    }

//    public KeycloakRoleResource role(String roleName){
//        return new KeycloakRoleResource(config, tokenManager, roleName, appName);
//    }

}
