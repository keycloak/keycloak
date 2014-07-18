package org.keycloak.admin.client.http.methods;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.admin.client.KeycloakException;
import org.keycloak.admin.client.http.AuthorizationHeader;
import org.keycloak.admin.client.token.TokenManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakHttpPut extends KeycloakHttpMethod{

    private HttpEntity entity;

    private boolean hasStringBody;

    public KeycloakHttpPut(String url, TokenManager tokenManager){
        super(url, tokenManager);
    }

    @Override
    public HttpResponse execute(){
        HttpResponse response = null;
        try {
            HttpPut put = new HttpPut(url);
            put.setEntity(entity);
            handleHeaders(put);

            response = client.execute(put);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
        return response;
    }

    public KeycloakHttpPut withBody(String body){
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

    public KeycloakHttpPut withBody(Map<String, String> formParams){
        throwIllegalArgumentExceptionIfNull(formParams, "Body cannot be null");

        try {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            for(Map.Entry<String, String> formParam : formParams.entrySet()){
                params.add(new BasicNameValuePair(formParam.getKey(), formParam.getValue()));
            }
            entity = new UrlEncodedFormEntity(params, "UTF-8");
            hasStringBody = false;
        } catch (UnsupportedEncodingException e) {
            throw new KeycloakException(e);
        }

        return this;
    }

    public KeycloakHttpPut withHeaders(Map<String, String> headers){
        this.headers.putAll(headers);
        return this;
    }

    private void handleHeaders(HttpPut put) {

        for(Map.Entry<String, String> header : headers.entrySet()){
            put.addHeader(header.getKey(), header.getValue());
        }

        if(addAuthorizationHeader && put.getFirstHeader("Authorization") == null){
            put.addHeader("Authorization", AuthorizationHeader.generateBearerHeader(tokenManager.getAccessTokenString()));
        }

        //In Keycloak, if we submit a post with a String body, it's always a JSON
        if(hasStringBody){
            put.setHeader("Content-Type", "application/json");
        }

    }

}
