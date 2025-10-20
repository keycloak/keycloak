package org.keycloak.protocol.ssf.keys;

import org.jboss.logging.Logger;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.JWKSHttpUtils;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;
import org.keycloak.util.JWKSUtils;

public class TransmitterPublicKeyLoader implements PublicKeyLoader {

    protected static final Logger log = Logger.getLogger(TransmitterPublicKeyLoader.class);

    protected final KeycloakSession session;

    protected final SsfTransmitterMetadata transmitterMetadata;

    public TransmitterPublicKeyLoader(KeycloakSession session, SsfTransmitterMetadata transmitterMetadata) {
        this.session = session;
        this.transmitterMetadata = transmitterMetadata;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        JSONWebKeySet jwks = JWKSHttpUtils.sendJwksRequest(session, transmitterMetadata.getJwksUri());
        return JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG, true);
    }
}
