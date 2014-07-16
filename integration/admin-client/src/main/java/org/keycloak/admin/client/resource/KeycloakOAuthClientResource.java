package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.OAuthClientService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.*;

import java.util.List;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakOAuthClientResource {

    private final String oAuthClientName;
    private final OAuthClientService oAuthClientService;

    public KeycloakOAuthClientResource(Config config, TokenManager tokenManager, String oAuthClientName){
        this.oAuthClientName = oAuthClientName;
        this.oAuthClientService = new OAuthClientService(config, tokenManager);
    }

    public OAuthClientRepresentation getRepresentation(){
        return oAuthClientService.find(oAuthClientName);
    }

    public ClaimRepresentation getClaims(){
        return oAuthClientService.getClaims(oAuthClientName);
    }

    public void updateClaims(ClaimRepresentation claimRepresentation){
        oAuthClientService.updateClaims(oAuthClientName, claimRepresentation);
    }

    public CredentialRepresentation generateNewSecret(){
        return oAuthClientService.generateNewSecret(oAuthClientName);
    }

    public CredentialRepresentation getSecret(){
        return oAuthClientService.getSecret(oAuthClientName);
    }

    public String getInstallationJson(){
        return oAuthClientService.getInstallationJson(oAuthClientName);
    }

    public MappingsRepresentation getScopeMappings(){
        return oAuthClientService.getScopeMappings(oAuthClientName);
    }

    public List<RoleRepresentation> getApplicationLevelRoles(String appName){
        return oAuthClientService.getApplicationLevelRoles(oAuthClientName, appName);
    }

    public void addApplicationLevelRoles(String appName, List<RoleRepresentation> rolesToAdd){
        oAuthClientService.addApplicationLevelRoles(oAuthClientName, appName, rolesToAdd);
    }

    public void removeApplicationLevelRoles(String appName, List<RoleRepresentation> rolesToRemove){
        oAuthClientService.removeApplicationLevelRoles(oAuthClientName, appName, rolesToRemove);
    }

    public List<RoleRepresentation> getAvailableApplicationLevelRoles(String appName){
        return oAuthClientService.getAvailableApplicationLevelRoles(oAuthClientName, appName);
    }

    public List<RoleRepresentation> getEffectiveApplicationLevelRoles(String appName){
        return oAuthClientService.getEffectiveApplicationLevelRoles(oAuthClientName, appName);
    }

    public List<RoleRepresentation> getRealmLevelRoles(){
        return oAuthClientService.getRealmLevelRoles(oAuthClientName);
    }

    public void addRealmLevelRoles(List<RoleRepresentation> rolesToAdd){
        oAuthClientService.addRealmLevelRoles(oAuthClientName, rolesToAdd);
    }

    public void removeRealmLevelRoles(List<RoleRepresentation> rolesToRemove){
        oAuthClientService.removeRealmLevelRoles(oAuthClientName, rolesToRemove);
    }

    public List<RoleRepresentation> getAvailableRealmLevelRoles(){
        return oAuthClientService.getAvailableRealmLevelRoles(oAuthClientName);
    }

    public List<RoleRepresentation> getEffectiveRealmLevelRoles(){
        return oAuthClientService.getEffectiveRealmLevelRoles(oAuthClientName);
    }

}
