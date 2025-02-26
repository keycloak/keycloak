package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.jose.jwk.JSONWebKeySet;

import java.io.IOException;

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

}
