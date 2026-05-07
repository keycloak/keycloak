package org.keycloak.tests.oid4vc;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.common.util.BouncyIntegration;
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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class OID4VCProofTestUtils {

    private OID4VCProofTestUtils() {
    }

    public static Proofs jwtProofs(String audience, String nonce) {
        return new Proofs().setJwt(List.of(generateJwtProof(audience, nonce)));
    }

    public static String generateJwtProof(String audience, String nonce) {
        return generateJwtProofWithClaims(List.of(audience), nonce, null, null, null, null, createEcKeyPair("proof-key"));
    }

    public static String generateJwtProof(String audience, KeyWrapper keyWrapper, String nonce) {
        return generateJwtProofWithClaims(List.of(audience), nonce, null, null, null, null, keyWrapper);
    }

    public static String generateJwtProofWithClaims(
            List<String> audiences,
            String nonce,
            String issuer,
            Long iat,
            Long exp,
            Long nbf
    ) {
        KeyWrapper keyWrapper = createEcKeyPair();
        return generateJwtProofWithClaims(audiences, nonce, issuer, iat, exp, nbf, keyWrapper);
    }

    private static String generateJwtProofWithClaims(
            List<String> audiences,
            String nonce,
            String issuer,
            Long iat,
            Long exp,
            Long nbf,
            KeyWrapper keyWrapper
    ) {
        keyWrapper.setKid(null);
        JWK jwk = JWKBuilder.create().ec(keyWrapper.getPublicKey());

        AccessToken token = new AccessToken();
        List<String> resolvedAudiences = audiences != null ? audiences : List.of();
        for (String audience : resolvedAudiences) {
            token.addAudience(audience);
        }
        token.setNonce(nonce);
        Optional.ofNullable(issuer).ifPresent(token::issuer);
        Optional.ofNullable(iat).ifPresentOrElse(token::iat, token::issuedNow);
        Optional.ofNullable(exp).ifPresent(token::exp);
        Optional.ofNullable(nbf).ifPresent(token::nbf);

        return new JWSBuilder()
                .type(JwtProofValidator.PROOF_JWT_TYP)
                .jwk(jwk)
                .jsonContent(token)
                .sign(new ECDSASignatureSignerContext(keyWrapper));
    }

    public static String generateJwtProofWithKidNoAttestation(String audience, String nonce) {
        KeyWrapper keyWrapper = createEcKeyPair();

        AccessToken token = new AccessToken();
        token.addAudience(audience);
        token.setNonce(nonce);
        token.issuedNow();

        return new JWSBuilder()
                .type(JwtProofValidator.PROOF_JWT_TYP)
                .kid(keyWrapper.getKid())
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
        KeyWrapper attestationKey = createEcKeyPair("attestation-key");
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
        long iatSeconds = System.currentTimeMillis() / 1000;
        body.setIat(iatSeconds);
        body.setExp(iatSeconds + 3600);
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

    public static KeyWrapper createEcKeyPair() {
        return createEcKeyPair("proof-key");
    }

    public static KeyWrapper newEcSigningKey(String keyId) {
        return createEcKeyPair(keyId);
    }

    public static KeyWrapper createEcKeyPair(String keyId) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BouncyIntegration.PROVIDER);
            kpg.initialize(256);
            var keyPair = kpg.generateKeyPair();

            KeyWrapper kw = new KeyWrapper();
            kw.setKid(keyId);
            kw.setUse(KeyUse.SIG);
            kw.setAlgorithm("ES256");
            kw.setType("EC");
            kw.setPublicKey(keyPair.getPublic());
            kw.setPrivateKey(keyPair.getPrivate());
            return kw;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyWrapper createRsaKeyPair(String keyId) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyIntegration.PROVIDER);
            kpg.initialize(2048);
            var keyPair = kpg.generateKeyPair();

            RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateCrtKey priv = (RSAPrivateCrtKey) keyPair.getPrivate();

            // Generate self-signed cert
            X500Name subject = new X500Name("CN=example.com");

            long now = System.currentTimeMillis();
            BigInteger serial = BigInteger.valueOf(now);
            Date notBefore = new Date(now);
            Date notAfter = new Date(now + 3600000); // 1h

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSAandMGF1")
                    .setProvider("BC")
                    .build(priv);
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subject, serial, notBefore, notAfter, subject, pub);
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(certBuilder.build(signer));

            KeyWrapper kw = new KeyWrapper();
            kw.setKid(keyId);
            kw.setUse(KeyUse.SIG);
            kw.setAlgorithm("PS256");
            kw.setType("RSA");

            kw.setPublicKey(pub);
            kw.setPrivateKey(priv);
            kw.setCertificate(cert);
            kw.setCertificateChain(List.of(cert));
            return kw;
        } catch (NoSuchAlgorithmException | OperatorCreationException | CertificateException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}
