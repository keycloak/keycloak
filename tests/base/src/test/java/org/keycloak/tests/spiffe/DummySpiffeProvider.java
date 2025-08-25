package org.keycloak.tests.spiffe;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

public class DummySpiffeProvider {

    private final KeyPair keyPair;

    private final JWK jwk;

    private final String jwksString;

    public DummySpiffeProvider() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
            keyPairGenerator.initialize(ecSpec);
            keyPair = keyPairGenerator.generateKeyPair();

            jwk = JWKBuilder.create().ec(keyPair.getPublic());

            JSONWebKeySet jwks = new JSONWebKeySet();
            jwks.setKeys(new JWK[] { jwk });
            jwksString = JsonSerialization.writeValueAsString(jwks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String generateToken(String subject, String audience) {
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }

        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.audience(audience);
        jsonWebToken.iat((long) Time.currentTime());
        jsonWebToken.exp(jsonWebToken.getIat() + 300);
        jsonWebToken.subject(subject);

        return new JWSBuilder().type("JWT").jsonContent(jsonWebToken).sign(new ECDSASignatureSignerContext(getKeyWrapper()));
    }

    public String getJwksString() {
        return jwksString;
    }

    private KeyWrapper getKeyWrapper() {
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setKid(jwk.getKeyId());
        keyWrapper.setPublicKey(keyPair.getPublic());
        keyWrapper.setPrivateKey(keyPair.getPrivate());
        keyWrapper.setUse(KeyUse.SIG);
        keyWrapper.setAlgorithm(Algorithm.ES256);
        return keyWrapper;
    }

}
