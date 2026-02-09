package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.jose.jwk.JSONWebKeySet;

import org.apache.http.client.methods.CloseableHttpResponse;

public class JwksResponse extends AbstractHttpResponse {

    private JSONWebKeySet jwks;

    JwksResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        jwks = asJson(JSONWebKeySet.class);
    }

    public JSONWebKeySet getJwks() {
        return jwks;
    }

    @Override
    protected void assertJsonContentType() throws IOException {
        String contentType = getContentType();
        if (contentType == null || !(contentType.startsWith("application/jwk-set+json") || contentType.startsWith("application/json"))) {
            throw new IOException("Invalid content type retrieved. Status: " + getStatusCode() + ", contentType: " + contentType);
        }
    }

}
