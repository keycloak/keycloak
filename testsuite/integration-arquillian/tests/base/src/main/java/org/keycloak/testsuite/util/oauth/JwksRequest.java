package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.HttpGet;
import org.keycloak.jose.jwk.JSONWebKeySet;

import java.io.IOException;

public class JwksRequest {

    private final OAuthClient client;

    public JwksRequest(OAuthClient client) {
        this.client = client;
    }

    public JSONWebKeySet send() throws IOException {
        HttpGet get = new HttpGet(client.getEndpoints().getJwks());
        get.addHeader("Accept", "application/json");
        return new JwksResponse(client.httpClient().get().execute(get)).getJwks();
    }

}
