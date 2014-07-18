package org.keycloak.admin.client.http.methods;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.keycloak.admin.client.token.TokenManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 *
 * This hierarchy isn't supposed to be a clean HTTP wrapper.
 * The idea is to have a group of classes that facilitates the
 * http requests regarding the communication with keycloak itself
 *
 */
public abstract class KeycloakHttpMethod {

    protected String url;
    protected Map<String, String> headers;
    protected HttpClient client;
    protected TokenManager tokenManager;
    protected boolean addAuthorizationHeader;

    public KeycloakHttpMethod(String url, TokenManager tokenManager){
        this.url = url;
        this.headers = new HashMap<String, String>();
        this.client = new DefaultHttpClient();
        this.tokenManager = tokenManager;
        this.addAuthorizationHeader = true;
    }

    public abstract HttpResponse execute();

    public KeycloakHttpMethod disableAutomaticAuthHeader(){
        addAuthorizationHeader = false;
        return this;
    }

    protected void throwIllegalArgumentExceptionIfNull(Object value, String msg) {
        if(value == null){
            throw new IllegalArgumentException(msg);
        }
    }

}
