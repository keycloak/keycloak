package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.HttpGet;
import org.keycloak.jose.jwk.JSONWebKeySet;

import java.io.IOException;

public class JwksRequest {

    private final AbstractOAuthClient<?> client;

    JwksRequest(AbstractOAuthClient<?> client) {
        this.client = client;
    }

    public JSONWebKeySet send() throws IOException {
        HttpGet get = new HttpGet(client.getEndpoints().getJwks());
        get.addHeader("Accept", "application/json");
        JwksResponse response = new JwksResponse(client.httpClient().get().execute(get));
        if (response.isSuccess()) {
            return response.getJwks();
        } else {
            throw new IOException("Failed to fetch keys: " + response.getStatusCode());
        }
    }

}
