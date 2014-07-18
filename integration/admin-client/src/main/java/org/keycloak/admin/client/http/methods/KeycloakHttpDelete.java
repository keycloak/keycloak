package org.keycloak.admin.client.http.methods;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.StringEntity;
import org.keycloak.admin.client.KeycloakException;
import org.keycloak.admin.client.http.AuthorizationHeader;
import org.keycloak.admin.client.token.TokenManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakHttpDelete extends KeycloakHttpMethod{
    
    private HttpEntity entity;

    private boolean hasStringBody;

    public KeycloakHttpDelete(String url, TokenManager tokenManager){
        super(url, tokenManager);
    }

    @Override
    public HttpResponse execute(){
        if(hasBody()){
            return executeWithBody();
        } else {
            return executeWithoutBody();
        }
    }
    
    public HttpResponse executeWithBody(){
        HttpResponse response = null;
        try {
            HttpDeleteWithBody delete = new HttpDeleteWithBody(url);
            delete.setEntity(entity);
            handleHeaders(delete);
            response = client.execute(delete);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
        return response;
    }

    public HttpResponse executeWithoutBody(){
        HttpResponse response = null;
        try {
            HttpDelete delete = new HttpDelete(url);
            handleHeaders(delete);

            response = client.execute(delete);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
        return response;
    }

    public KeycloakHttpDelete withBody(String body){
        throwIllegalArgumentExceptionIfNull(body, "Body cannot be null");

        try {
            StringEntity stringEntity = new StringEntity(body, "UTF-8");
            stringEntity.setContentType("application/json");
            entity = stringEntity;
            hasStringBody = true;
        } catch (UnsupportedEncodingException e) {
            throw new KeycloakException(e);
        }

        return this;
    }

    public KeycloakHttpDelete withHeaders(Map<String, String> headers){
        this.headers.putAll(headers);
        return this;
    }

    private void handleHeaders(HttpDelete delete) {
        for(Map.Entry<String, String> header : headers.entrySet()){
            delete.addHeader(header.getKey(), header.getValue());
        }

        if(addAuthorizationHeader && delete.getFirstHeader("Authorization") == null){
            delete.addHeader("Authorization", AuthorizationHeader.generateBearerHeader(tokenManager.getAccessTokenString()));
        }

        //In Keycloak, if we submit a post with a String body, it's always a JSON
        if(hasStringBody){
            delete.setHeader("Content-Type", "application/json");
        }
    }

    private void handleHeaders(HttpDeleteWithBody delete) {

        for(Map.Entry<String, String> header : headers.entrySet()){
            delete.addHeader(header.getKey(), header.getValue());
        }

        if(addAuthorizationHeader && delete.getFirstHeader("Authorization") == null){
            delete.addHeader("Authorization", AuthorizationHeader.generateBearerHeader(tokenManager.getAccessTokenString()));
        }

        //In Keycloak, if we submit a post with a String body, it's always a JSON
        if(delete.getFirstHeader("Content-Type") == null){
            delete.addHeader("Content-Type", "application/json");
        }

    }
    
    private boolean hasBody(){
        return entity != null;
    }

}
