package org.keycloak.broker.spiffe;

import java.net.URI;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JWKSUtils;

import static org.keycloak.connections.httpclient.DefaultHttpClientFactory.METRICS_URI_TEMPLATE_HEADER;

public class SpiffeBundleEndpointLoader implements PublicKeyLoader {

    private final KeycloakSession session;
    private final String bundleEndpoint;

    public SpiffeBundleEndpointLoader(KeycloakSession session, String bundleEndpoint) {
        this.session = session;
        this.bundleEndpoint = bundleEndpoint;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        URI uri = URI.create(bundleEndpoint);
        SpiffeJSONWebKeySet jwks = SimpleHttp.create(session).doGet(bundleEndpoint)
              .header(METRICS_URI_TEMPLATE_HEADER, uri.getPath())
              .asJson(SpiffeJSONWebKeySet.class);
        PublicKeysWrapper keysWrapper = JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.JWT_SVID, true);
        if (keysWrapper.getKeys().isEmpty()) {
            keysWrapper = JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
        }
        return jwks.getSpiffeRefreshHint() == null ? keysWrapper : new PublicKeysWrapper(keysWrapper.getKeys(), jwks.getSpiffeRefreshHint());
    }

}
