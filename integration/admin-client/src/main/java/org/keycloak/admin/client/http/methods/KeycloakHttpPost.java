package org.keycloak.admin.client.http.methods;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
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
public class KeycloakHttpPost extends KeycloakHttpMethod {

    private HttpEntity entity;

    private boolean hasStringBody;

    public KeycloakHttpPost(String url, TokenManager tokenManager){
        super(url, tokenManager);
    }

    @Override
    public HttpResponse execute(){
        HttpResponse response = null;
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(entity);
            handleHeaders(post);

            response = client.execute(post);
        } catch (IOException e) {
            throw new KeycloakException(e);
        }
        return response;
    }

    public KeycloakHttpPost withBody(String body){
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

    public KeycloakHttpPost withBody(Map<String, String> formParams){
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

    public KeycloakHttpPost withHeaders(Map<String, String> headers){
        this.headers.putAll(headers);
        return this;
    }

    private void handleHeaders(HttpPost post) {

        for(Map.Entry<String, String> header : headers.entrySet()){
            post.addHeader(header.getKey(), header.getValue());
        }

        if(addAuthorizationHeader && post.getFirstHeader("Authorization") == null){
            post.addHeader("Authorization", AuthorizationHeader.generateBearerHeader(tokenManager.getAccessTokenString()));
        }

        //In Keycloak, if we submit a post with a String body, it's always a JSON
        if(hasStringBody){
            post.setHeader("Content-Type", "application/json");
        }

    }

}
