package org.keycloak.broker.spiffe;

import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

public class SpiffeBundleEndpointLoader implements PublicKeyLoader {

    private final KeycloakSession session;
    private final String bundleEndpoint;
    private final String trustDomain;

    public SpiffeBundleEndpointLoader(KeycloakSession session, String bundleEndpoint) {
        this(session, bundleEndpoint, null);
    }

    public SpiffeBundleEndpointLoader(KeycloakSession session, String bundleEndpoint, String trustDomain) {
        this.session = session;
        this.bundleEndpoint = bundleEndpoint;
        this.trustDomain = trustDomain == null ? null : trustDomain.substring("spiffe://".length());
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        String response = SimpleHttp.create(session).doGet(bundleEndpoint).asString();
        JsonNode bundle = JsonSerialization.mapper.reader().with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(response);
        if (bundle == null || !bundle.isObject()) {
            throw new IllegalArgumentException("Invalid SPIFFE bundle response");
        }
        boolean bundleMap = bundle.has("trust_domains");
        if (bundleMap) {
            JsonNode trustDomains = bundle.get("trust_domains");
            if (!trustDomains.isObject()) {
                throw new IllegalArgumentException("Invalid SPIFFE bundle map");
            }
            JsonNode selected = trustDomain == null ? null : trustDomains.get(trustDomain);
            if (selected == null || !selected.isObject()) {
                throw new IllegalArgumentException("SPIFFE trust domain not found in bundle map");
            }
            bundle = selected;
        }
        if (!bundle.isObject() || !bundle.path("keys").isArray()) {
            throw new IllegalArgumentException("Invalid SPIFFE bundle");
        }
        SpiffeJSONWebKeySet jwks = JsonSerialization.mapper.treeToValue(bundle, SpiffeJSONWebKeySet.class);
        PublicKeysWrapper keysWrapper = JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.JWT_SVID, true);
        if (keysWrapper.getKeys().isEmpty()) {
            keysWrapper = JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
        }
        if (bundleMap) {
            return new PublicKeysWrapper(keysWrapper.getKeys(), Time.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
        }
        return jwks.getSpiffeRefreshHint() == null ? keysWrapper : new PublicKeysWrapper(keysWrapper.getKeys(), Time.currentTimeMillis() + TimeUnit.SECONDS.toMillis(jwks.getSpiffeRefreshHint()));
    }

}
