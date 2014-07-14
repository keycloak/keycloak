package org.keycloak.admin.client.http;

import org.keycloak.admin.client.http.methods.KeycloakHttpDelete;
import org.keycloak.admin.client.http.methods.KeycloakHttpGet;
import org.keycloak.admin.client.http.methods.KeycloakHttpPost;
import org.keycloak.admin.client.http.methods.KeycloakHttpPut;
import org.keycloak.admin.client.token.TokenManager;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakHttp {

    private TokenManager tokenManager;

    public KeycloakHttp(TokenManager tokenManager){
        this.tokenManager = tokenManager;
    }

    public KeycloakHttpGet get(String url){
        return new KeycloakHttpGet(url, tokenManager);
    }

    public KeycloakHttpPost post(String url){
        return new KeycloakHttpPost(url, tokenManager);
    }

    public KeycloakHttpPut put(String url){
        return new KeycloakHttpPut(url, tokenManager);
    }

    public KeycloakHttpDelete delete(String url){
        return new KeycloakHttpDelete(url, tokenManager);
    }

}
