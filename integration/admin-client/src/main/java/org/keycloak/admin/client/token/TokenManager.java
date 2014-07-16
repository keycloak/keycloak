package org.keycloak.admin.client.token;

import org.keycloak.admin.client.Config;
import org.keycloak.admin.client.KeycloakException;
import org.keycloak.admin.client.URI;
import org.keycloak.admin.client.http.AuthorizationHeader;
import org.keycloak.admin.client.http.KeycloakHttp;
import org.keycloak.admin.client.json.JsonSerialization;
import org.apache.http.HttpEntity;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessTokenResponse;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class TokenManager {

    private AccessTokenResponse currentToken;
    private Date expirationTime;
    private KeycloakHttp http;
    private Config config;

    public TokenManager(Config config){
        http = new KeycloakHttp(null);
        this.config = config;
    }

    public String getAccessTokenString(){
        return getAccessToken().getToken();
    }

    public AccessTokenResponse getAccessToken(){
        if(currentToken == null){
            grantToken();
        }else if(tokenExpired()){
            refreshToken();
        }
        return currentToken;
    }

    public AccessTokenResponse grantToken(){
        AccessTokenResponse response = null;

        String url = URI.TOKENS_DIRECT_GRANT.build(config.getServerUrl(), config.getRealm());
        Map<String, String> headers = new HashMap<String, String>();
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", config.getUsername());
        params.put("password", config.getPassword());

        if(config.isPublicClient()){
            params.put("client_id", config.getClientId());
        } else {
            headers.put("Authorization", AuthorizationHeader.generateBasicHeader(config.getClientId(), config.getClientSecret()));
        }

        HttpEntity entity = http.post(url).withHeaders(headers).withBody(params).disableAutomaticAuthHeader().execute().getEntity();

        try {
            response = JsonSerialization.readValue(entity.getContent(), AccessTokenResponse.class);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }

        defineCurrentToken(response);
        return response;

    }

    public AccessTokenResponse refreshToken(){
        AccessTokenResponse response = null;

        String url = URI.TOKENS_REFRESH.build(config.getServerUrl(), config.getRealm());
        Map<String, String> headers = new HashMap<String, String>();
        Map<String, String> params = new HashMap<String, String>();
        params.put(OAuth2Constants.REFRESH_TOKEN, currentToken.getRefreshToken());

        if(config.isPublicClient()){
            params.put("client_id", config.getClientId());
        } else {
            headers.put("Authorization", AuthorizationHeader.generateBasicHeader(config.getClientId(), config.getClientSecret()));
        }

        HttpEntity entity = http.post(url).withHeaders(headers).withBody(params).disableAutomaticAuthHeader().execute().getEntity();

        try {
            response = JsonSerialization.readValue(entity.getContent(), AccessTokenResponse.class);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }

        defineCurrentToken(response);
        return response;
    }

    private void setExpirationTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, (int) currentToken.getExpiresIn());
        expirationTime = cal.getTime();
    }

    private boolean tokenExpired() {
        return new Date().after(expirationTime);
    }

    private void defineCurrentToken(AccessTokenResponse accessTokenResponse){
        currentToken = accessTokenResponse;
        setExpirationTime();
    }

}
