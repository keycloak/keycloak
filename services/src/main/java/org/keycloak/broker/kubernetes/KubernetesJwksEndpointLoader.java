package org.keycloak.broker.kubernetes;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.keycloak.broker.kubernetes.KubernetesConstants.SERVICE_ACCOUNT_TOKEN_PATH;

public class KubernetesJwksEndpointLoader implements PublicKeyLoader {

    private final KeycloakSession session;

    private final boolean authenticate;
    private final String endpoint;

    public KubernetesJwksEndpointLoader(KeycloakSession session, String globalEndpoint, String providerEndpoint) {
        this.session = session;

        if (globalEndpoint == null && providerEndpoint == null) {
            throw new RuntimeException("Not running on Kubernetes and Kubernetes JWKS endpoint not set");
        }

        if (providerEndpoint == null || providerEndpoint.isEmpty() || globalEndpoint.equals(providerEndpoint)) {
            this.endpoint = globalEndpoint;
            authenticate = true;
        } else {
            this.endpoint = providerEndpoint;
            authenticate = false;
        }
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpGet httpGet = new HttpGet(endpoint);

        httpGet.setHeader(HttpHeaders.ACCEPT, "application/jwk-set+json");

        if (authenticate) {
            String token = FileUtils.readFileToString(new File(SERVICE_ACCOUNT_TOKEN_PATH), StandardCharsets.UTF_8);
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            JSONWebKeySet jwks = JsonSerialization.readValue(response.getEntity().getContent(), JSONWebKeySet.class);
            return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG);
        }
    }
}
