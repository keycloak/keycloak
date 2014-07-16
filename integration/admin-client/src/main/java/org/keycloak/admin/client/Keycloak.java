package org.keycloak.admin.client;

import org.keycloak.admin.client.resource.*;
import org.keycloak.admin.client.service.KeycloakServices;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class Keycloak {

    private final Config config;
    private final TokenManager tokenManager;

    private Keycloak(String serverUrl, String realm, String username, String password, String clientId, String clientSecret){
        config = new Config(serverUrl, realm, username, password, clientId, clientSecret);
        tokenManager = new TokenManager(config);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId, String clientSecret){
        return new Keycloak(serverUrl, realm, username, password, clientId, clientSecret);
    }

    public static Keycloak getInstance(String serverUrl, String realm, String username, String password, String clientId){
        return new Keycloak(serverUrl, realm, username, password, clientId, null);
    }

    public KeycloakApplicationsResource applications(){
        return new KeycloakApplicationsResource(config, tokenManager);
    }

    public KeycloakApplicationResource application(ApplicationRepresentation applicationRep){
        return application(applicationRep.getName());
    }

    public KeycloakApplicationResource application(String appName){
        return new KeycloakApplicationResource(config, tokenManager, appName);
    }

    public KeycloakOAuthClientsResource oAuthClients(){
        return new KeycloakOAuthClientsResource(config, tokenManager);
    }

    public KeycloakOAuthClientResource oAuthClient(OAuthClientRepresentation oAuthClientRep){
        return oAuthClient(oAuthClientRep.getName());
    }

    public KeycloakOAuthClientResource oAuthClient(String oAuthClientName){
        return new KeycloakOAuthClientResource(config, tokenManager, oAuthClientName);
    }

    public KeycloakRealmResource realm(){
        return new KeycloakRealmResource(config, tokenManager);
    }

    public KeycloakUsersResource users(){
        return new KeycloakUsersResource(config, tokenManager);
    }

    public KeycloakUserResource user(UserRepresentation userRep){
        return user(userRep.getUsername());
    }

    public KeycloakUserResource user(String username){
        return new KeycloakUserResource(config, tokenManager, username);
    }

    public TokenManager tokenManager(){
        return tokenManager;
    }

    public KeycloakServices services(){
        return new KeycloakServices(config, tokenManager);
    }

}
