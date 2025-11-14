package org.keycloak.broker.jwtauthorizationgrant;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JWKSUtils;


public class JWTAuthorizationGrantJWKSEndpointLoader implements PublicKeyLoader {

    private final KeycloakSession session;
    private final String jwksUrl;

    public JWTAuthorizationGrantJWKSEndpointLoader(KeycloakSession session, String jwksUrl) {
        this.session = session;
        this.jwksUrl = jwksUrl;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        JSONWebKeySet jwks = SimpleHttp.create(session).doGet(jwksUrl).asJson(JSONWebKeySet.class);
        return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
    }

}
