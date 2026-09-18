package org.keycloak.broker.kubernetes;

import java.io.IOException;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.Strings;

import org.apache.http.HttpHeaders;

public class KubernetesJwksEndpointLoader implements PublicKeyLoader {

    private final KeycloakSession session;
    private final String issuer;
    private final String jwksUrl;

    public KubernetesJwksEndpointLoader(KeycloakSession session, String issuer) {
        this(session, issuer, null);
    }

    public KubernetesJwksEndpointLoader(KeycloakSession session, String issuer, String jwksUrl) {
        this.session = session;
        this.issuer = issuer;
        this.jwksUrl = jwksUrl;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        SimpleHttp simpleHttp = SimpleHttp.create(session);

        String token = getToken(issuer);
        String jwksUri = jwksUrl;
        if (Strings.isEmpty(jwksUri)) {
            String wellKnownEndpoint = KubernetesUtils.discoveryUrl(issuer);
            SimpleHttpRequest wellKnownRequest = simpleHttp.doGet(wellKnownEndpoint).acceptJson();
            if (token != null) {
                wellKnownRequest.auth(token);
            }

            jwksUri = wellKnownRequest.asJson(OIDCConfigurationRepresentation.class).getJwksUri();
            if (jwksUri == null) {
                throw new IOException("OIDC discovery document from " + wellKnownEndpoint + " did not include a jwks_uri");
            }
        }

        SimpleHttpRequest jwksRequest = simpleHttp.doGet(jwksUri).header(HttpHeaders.ACCEPT, "application/jwk-set+json");
        if (token != null) {
            jwksRequest.auth(token);
        }

        JSONWebKeySet jwks = jwksRequest.asJson(JSONWebKeySet.class);
        return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG);
    }

    private String getToken(String issuer) {
        return KubernetesUtils.getToken(issuer);
    }
}
