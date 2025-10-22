package org.keycloak.broker.spiffe;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
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
        SpiffeJSONWebKeySet jwks = SimpleHttp.create(session).doGet(bundleEndpoint).asJson(SpiffeJSONWebKeySet.class);
        PublicKeysWrapper keysWrapper = JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.JWT_SVID, true);
        if (keysWrapper.getKeys().isEmpty()) {
            keysWrapper = JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
        }
        return jwks.getSpiffeRefreshHint() == null ? keysWrapper : new PublicKeysWrapper(keysWrapper.getKeys(), jwks.getSpiffeRefreshHint());
    }

}
