package org.keycloak.admin.client;

import org.keycloak.admin.client.resource.KeycloakAdminFactory;
import org.keycloak.admin.client.resource.KeycloakRealm;
import org.keycloak.admin.client.token.TokenManager;

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

    public KeycloakRealm realm(String realmName){
        return KeycloakAdminFactory.getRealm(config, tokenManager, realmName);
    }

    public TokenManager tokenManager(){
        return tokenManager;
    }

}
