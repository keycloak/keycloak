package org.keycloak.examples.authn.delegate.util.httpclient;

public class BackChannelHttpResponse {
    
    int statusCode = 0;
    String body = null;
    

    BackChannelHttpResponse(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }    
    
    public int getStatusCode() {
        return statusCode;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
    public String getBody() {
        return body;
    }

}
