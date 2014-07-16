package org.keycloak.admin.client.service;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.token.TokenManager;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakServices {

    private final Config config;
    private final TokenManager tokenManager;

    public KeycloakServices(Config config, TokenManager tokenManager) {
        this.config = config;
        this.tokenManager = tokenManager;
    }

    public ApplicationService applicationService(){
        return new ApplicationService(config, tokenManager);
    }

    public OAuthClientService oAuthClientService(){
        return new OAuthClientService(config, tokenManager);
    }

    public RealmService realmService(){
        return new RealmService(config, tokenManager);
    }

    public RoleService roleService(){
        return new RoleService(config, tokenManager);
    }

    public UserService userService(){
        return new UserService(config, tokenManager);
    }

}
