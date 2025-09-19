package org.keycloak.broker.kube;

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

public class KubeJwksEndpointLoader implements PublicKeyLoader {

    private final KeycloakSession session;
    private final String endpoint;

    public KubeJwksEndpointLoader(KeycloakSession session, String endpoint) {
        this.session = session;
        this.endpoint = endpoint;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpGet httpGet = new HttpGet(endpoint);

        String token = FileUtils.readFileToString(new File("/var/run/secrets/kubernetes.io/serviceaccount/token"), StandardCharsets.UTF_8);

        httpGet.setHeader(HttpHeaders.ACCEPT, "application/jwk-set+json");
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            JSONWebKeySet jwks = JsonSerialization.readValue(response.getEntity().getContent(), JSONWebKeySet.class);
            return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG);
        }
    }
}
