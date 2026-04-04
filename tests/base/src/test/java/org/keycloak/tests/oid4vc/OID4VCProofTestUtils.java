package org.keycloak.tests.oid4vc;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtProofValidator;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.representations.AccessToken;

public final class OID4VCProofTestUtils {

    private OID4VCProofTestUtils() {
    }

    public static Proofs jwtProofs(String audience, String nonce) {
        return new Proofs().setJwt(List.of(generateJwtProof(audience, nonce)));
    }

    public static String generateJwtProof(String audience, String nonce) {
        return generateJwtProof(audience, createEcKeyPair(), nonce);
    }

    public static String generateJwtProof(String audience, KeyWrapper keyWrapper, String nonce) {
        keyWrapper.setKid(null);
        JWK jwk = JWKBuilder.create().ec(keyWrapper.getPublicKey());

        AccessToken token = new AccessToken();
        token.addAudience(audience);
        token.setNonce(nonce);
        token.issuedNow();

        return new JWSBuilder()
                .type(JwtProofValidator.PROOF_JWT_TYP)
                .jwk(jwk)
                .jsonContent(token)
                .sign(new ECDSASignatureSignerContext(keyWrapper));
    }

    public static Proofs attestationProofs(String nonce, List<JWK> attestedKeys, List<String> keyStorage, List<String> userAuthentication) {
        return new Proofs().setAttestation(List.of(generateAttestationProof(nonce, attestedKeys, keyStorage, userAuthentication)));
    }

    public static String generateAttestationProof(
            String nonce,
            List<JWK> attestedKeys,
            List<String> keyStorage,
            List<String> userAuthentication
    ) {
        KeyWrapper attestationKey = newEcSigningKey("attestation-key");
        return generateAttestationProof(attestationKey, nonce, attestedKeys, keyStorage, userAuthentication, null);
    }

    public static String generateAttestationProof(
            KeyWrapper attestationKey,
            String nonce,
            List<JWK> attestedKeys,
            List<String> keyStorage,
            List<String> userAuthentication,
            String certification
    ) {
        KeyAttestationJwtBody body = new KeyAttestationJwtBody();
        body.setIat((long) (System.currentTimeMillis() / 1000));
        body.setNonce(nonce);
        body.setAttestedKeys(attestedKeys);
        body.setKeyStorage(keyStorage);
        body.setUserAuthentication(userAuthentication);
        body.setCertification(certification);
        body.setStatus(Map.of("status", "valid"));

        return new JWSBuilder()
                .type(AttestationValidatorUtil.ATTESTATION_JWT_TYP)
                .kid(attestationKey.getKid())
                .jsonContent(body)
                .sign(new ECDSASignatureSignerContext(attestationKey));
    }

    public static KeyWrapper newEcSigningKey(String keyId) {
        KeyWrapper kw = createEcKeyPair();
        if (keyId != null && !keyId.isBlank()) {
            kw.setKid(keyId);
        }
        return kw;
    }

    public static KeyWrapper createEcKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BouncyIntegration.PROVIDER);
            kpg.initialize(256);
            var keyPair = kpg.generateKeyPair();

            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(KeyUse.SIG);
            kw.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
            kw.setType("EC");
            kw.setAlgorithm("ES256");
            return kw;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
