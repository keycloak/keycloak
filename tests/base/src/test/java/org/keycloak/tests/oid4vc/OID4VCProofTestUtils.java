package org.keycloak.tests.oid4vc;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.protocol.oid4vc.issuance.keybinding.AttestationValidatorUtil;
import org.keycloak.protocol.oid4vc.issuance.keybinding.JwtProofValidator;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oidc.utils.JWKSServerUtils;
import org.keycloak.representations.AccessToken;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
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

    public static String generateJwtProof(String audience, String nonce, KeyWrapper keyWrapper) {
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
        jwk.setAlgorithm(keyWrapper.getAlgorithm());

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

    public static X509Certificate createCaCertificate(KeyPair caKeyPair, String commonName) {
        X500Name caName = new X500Name("CN=" + commonName);
        try {
            X509v3CertificateBuilder builder = certificateBuilder(caName, caName, caKeyPair);
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            return signCertificate(builder, caKeyPair);
        } catch (CertIOException | OperatorCreationException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static X509Certificate createEndEntityCertificate(KeyPair subjectKeyPair, KeyPair caKeyPair,
            X509Certificate caCertificate, String commonName) {
        X500Name issuer = new X500Name(caCertificate.getSubjectX500Principal().getName());
        X500Name subject = new X500Name("CN=" + commonName);
        try {
            X509v3CertificateBuilder builder = certificateBuilder(issuer, subject, subjectKeyPair);
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            return signCertificate(builder, caKeyPair);
        } catch (CertIOException | OperatorCreationException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private static X509v3CertificateBuilder certificateBuilder(X500Name issuer, X500Name subject,
            KeyPair subjectKeyPair) {
        Instant now = Instant.now();
        return new X509v3CertificateBuilder(
                issuer,
                new BigInteger(160, new SecureRandom()),
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject,
                SubjectPublicKeyInfo.getInstance(subjectKeyPair.getPublic().getEncoded()));
    }

    private static X509Certificate signCertificate(X509v3CertificateBuilder builder, KeyPair signingKeyPair)
            throws OperatorCreationException, CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(signingKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    public static JSONWebKeySet toJwks(KeyWrapper... keys) {
        List<JWK> jwkList = Arrays.stream(keys)
                .map(JWKSServerUtils::toJwk)
                .filter(Objects::nonNull)
                .toList();

        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(jwkList.toArray(new JWK[0]));

        return jwks;
    }
}
