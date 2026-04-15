package org.keycloak.ssf.receiver.keys;

import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.JWKSHttpUtils;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.util.JWKSUtils;

/**
 * {@link PublicKeyLoader} to fetch the public Keycloak from the SSF Transmitter metadata.
 */
public class TransmitterPublicKeyLoader implements PublicKeyLoader {

    protected final KeycloakSession session;

    protected String jwksUri;

    public TransmitterPublicKeyLoader(KeycloakSession session, String jwksUri) {
        this.session = session;
        this.jwksUri = jwksUri;
    }

    public TransmitterPublicKeyLoader(KeycloakSession session, TransmitterMetadata transmitterMetadata) {
        this(session, transmitterMetadata.getJwksUri());
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, jwksUri);
        return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
    }
}
