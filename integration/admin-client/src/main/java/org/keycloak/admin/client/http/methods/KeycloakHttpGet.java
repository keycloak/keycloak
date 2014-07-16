package org.keycloak.admin.client.http.methods;

import org.keycloak.admin.client.http.AuthorizationHeader;
import org.keycloak.admin.client.json.JsonSerialization;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.admin.client.utils.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public class KeycloakHttpGet extends KeycloakHttpMethod {

    public KeycloakHttpGet(String url, TokenManager tokenManager){
        super(url, tokenManager);
    }

    @SuppressWarnings("RedundantCast")
    public <T> T getTypedResponse(Class<T> returnType){
        T responseElement = null;

        try {
            InputStream content = execute().getEntity().getContent();
            responseElement = (T) JsonSerialization.readValue(content, returnType);
        } catch (JsonParseException e){
            responseElement = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseElement;
    }

    @SuppressWarnings("RedundantCast")
    public <T> T getTypedResponse(TypeReference typeReference){
        T responseElement = null;

        try {
            InputStream content = execute().getEntity().getContent();
            responseElement = (T) JsonSerialization.readValue(content, typeReference);
        } catch (JsonParseException e){
            responseElement = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseElement;
    }

    @SuppressWarnings("RedundantCast")
    public <T> T getTypedResponse(CollectionType collectionType){
        T responseElement = null;

        try {
            InputStream content = execute().getEntity().getContent();
            responseElement = (T) JsonSerialization.readValue(content, collectionType);
        } catch (JsonParseException e){
            responseElement = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseElement;
    }

    public String getStringResponse(){
        String responseString = null;

        try {
            InputStream content = execute().getEntity().getContent();
            responseString = StringUtils.inputStreamToString(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseString;
    }

    @Override
    public HttpResponse execute(){
        HttpResponse response = null;

        try {
            HttpGet get = new HttpGet(url);
            handleHeaders(get);

            response = client.execute(get);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    public KeycloakHttpGet withHeaders(Map<String, String> headers){
        this.headers.putAll(headers);
        return this;
    }

    private void handleHeaders(HttpGet get) {

        for(Map.Entry<String, String> header : headers.entrySet()){
            get.addHeader(header.getKey(), header.getValue());
        }

        if(addAuthorizationHeader && get.getFirstHeader("Authorization") == null){
            get.addHeader("Authorization", AuthorizationHeader.generateBearerHeader(tokenManager.getAccessTokenString()));
        }
    }

}
