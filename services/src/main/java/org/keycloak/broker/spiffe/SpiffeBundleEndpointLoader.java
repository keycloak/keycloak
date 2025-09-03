package org.keycloak.broker.spiffe;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.JWKSHttpUtils;
import org.keycloak.util.JWKSUtils;

public class SpiffeBundleEndpointLoader implements PublicKeyLoader {

    private final KeycloakSession session;
    private final String bundleEndpoint;

    public SpiffeBundleEndpointLoader(KeycloakSession session, String bundleEndpoint) {
        this.session = session;
        this.bundleEndpoint = bundleEndpoint;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, bundleEndpoint);
        return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.JWT_SVID);
    }

}
