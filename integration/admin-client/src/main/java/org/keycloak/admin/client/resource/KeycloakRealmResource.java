package org.keycloak.admin.client.resource;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.service.RealmService;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.adapters.action.SessionStats;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakRealmResource {

    private final RealmService realmService;
    private final Config config;
    private final TokenManager tokenManager;
    private KeycloakRealmLevelRolesResource realmLevelRoleService;

    public KeycloakRealmResource(Config config, TokenManager tokenManager){
        this.config = config;
        this.tokenManager = tokenManager;
        this.realmService = new RealmService(config, tokenManager);
    }

    public RealmRepresentation getRepresentation(){
        return realmService.getRealm();
    }

    public void logoutAllSessions(){
        realmService.logoutAllSessions();
    }

    public Map<String, SessionStats> getSessionStats(){
        return realmService.getSessionStats();
    }

    public void removeUserSession(String sessionId){
        realmService.removeUserSession(sessionId);
    }

    public KeycloakRealmLevelRolesResource roles(){
        if(realmLevelRoleService == null){
            realmLevelRoleService = new KeycloakRealmLevelRolesResource(config, tokenManager);
        }
        return realmLevelRoleService;
    }

    public KeycloakRoleResource role(String roleName){
        return new KeycloakRoleResource(config, tokenManager, roleName);
    }

}
