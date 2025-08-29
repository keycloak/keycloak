package org.keycloak.util;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.representations.dpop.DPoP;

import static org.keycloak.OAuth2Constants.DPOP_DEFAULT_ALGORITHM;
import static org.keycloak.OAuth2Constants.DPOP_JWT_HEADER_TYPE;
import static org.keycloak.jose.jwk.JWKUtil.toIntegerBytes;

/**
 * Utility for generating signed DPoP proofs
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9449">OAuth 2.0 Demonstrating Proof of Possession (DPoP) specification</a>
 *
 */
public class DPoPGenerator {

    // TODO: Similar for EC and EdDSA
    public static String generateRsaSignedDPoPProof(KeyPair rsaKeyPair, String httpMethod, String endpointURL, String accessToken) {
        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(DPOP_DEFAULT_ALGORITHM, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        return generateSignedDPoPProof(SecretGenerator.getInstance().generateSecureID(), httpMethod, endpointURL, (long) Time.currentTime(), DPOP_DEFAULT_ALGORITHM.toString(),
                jwsRsaHeader, rsaKeyPair.getPrivate(), accessToken);
    }


    public static JWK createRsaJwk(Key publicKey) {
        RSAPublicKey rsaKey = (RSAPublicKey) publicKey;

        RSAPublicJWK k = new RSAPublicJWK();
        k.setKeyType(KeyType.RSA);
        k.setModulus(Base64Url.encode(toIntegerBytes(rsaKey.getModulus())));
        k.setPublicExponent(Base64Url.encode(toIntegerBytes(rsaKey.getPublicExponent())));

        return k;
    }


    public static String generateSignedDPoPProof(String jti, String htm, String htu, Long iat, String algorithm, JWSHeader jwsHeader, PrivateKey privateKey, String accessToken) {
        try {
            String dpopProofHeaderEncoded = Base64Url.encode(JsonSerialization.writeValueAsBytes(jwsHeader));

            DPoP dpop = new DPoP();
            dpop.id(jti);
            dpop.setHttpMethod(htm);
            dpop.setHttpUri(htu);
            dpop.iat(iat);
            if (accessToken != null) {
                dpop.setAccessTokenHash(HashUtils.accessTokenHash(OAuth2Constants.DPOP_DEFAULT_ALGORITHM.toString(), accessToken, true));
            }

            String dpopProofPayloadEncoded = Base64Url.encode(JsonSerialization.writeValueAsBytes(dpop));


            KeyWrapper keyWrapper = new KeyWrapper();
            keyWrapper.setKid(jwsHeader.getKeyId());
            keyWrapper.setAlgorithm(algorithm);
            keyWrapper.setPrivateKey(privateKey);
            keyWrapper.setType(privateKey.getAlgorithm());
            keyWrapper.setUse(KeyUse.SIG);
            SignatureSignerContext sigCtx = createSignatureSignerContext(keyWrapper);

            String data = dpopProofHeaderEncoded + "." + dpopProofPayloadEncoded;
            byte[] signatureByteArray = sigCtx.sign(data.getBytes());
            return data + "." + Base64Url.encode(signatureByteArray);
        } catch (SignatureException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SignatureSignerContext createSignatureSignerContext(KeyWrapper keyWrapper) {
        switch (keyWrapper.getType()) {
            case KeyType.RSA:
                return new AsymmetricSignatureSignerContext(keyWrapper);
            case KeyType.EC:
                return new ECDSASignatureSignerContext(keyWrapper);
             // TODO: EdDSA?
            default:
                throw new IllegalArgumentException("No signer provider for key algorithm type " + keyWrapper.getType());
        }
    }
}
