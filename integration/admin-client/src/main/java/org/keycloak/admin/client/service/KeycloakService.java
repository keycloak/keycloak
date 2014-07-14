package org.keycloak.admin.client.service;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.http.KeycloakHttp;
import org.keycloak.admin.client.token.TokenManager;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public abstract class KeycloakService {

    protected KeycloakHttp http;
    protected Config config;
    protected TokenManager tokenManager;

    public KeycloakService(Config config, TokenManager tokenManager){
        http = new KeycloakHttp(tokenManager);
        this.config = config;
        this.tokenManager = tokenManager;
    }

}
