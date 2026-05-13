package org.keycloak.tests.oid4vc.presentation;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;

public class TestCredentialBuilder {

    private static final String ISSUER = "https://issuer.example.org";
    private static final String DEFAULT_CREDENTIAL_TYPE = "urn:keycloak:oid4vp:credential";
    private static final List<String> VISIBLE_CLAIMS = List.of(CLAIM_NAME_IAT);

    private final KeyWrapper issuerKey;
    private final Map<String, Object> claims = new LinkedHashMap<>();

    private String issuer = ISSUER;
    private String credentialType = DEFAULT_CREDENTIAL_TYPE;
    private boolean includeKeyId = true;
    private boolean includeX5cHeader = true;

    private TestCredentialBuilder(KeyWrapper issuerKey) {
        this.issuerKey = issuerKey;
    }

    static TestCredentialBuilder create() {
        return new TestCredentialBuilder(OID4VCProofTestUtils.createRsaKeyPair("issuer-" + UUID.randomUUID()));
    }

    TestCredentialBuilder withIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    TestCredentialBuilder withCredentialType(String credentialType) {
        this.credentialType = credentialType;
        return this;
    }

    TestCredentialBuilder withClaim(String name, Object value) {
        claims.put(name, value);
        return this;
    }

    TestCredentialBuilder withClaims(Map<String, Object> claims) {
        this.claims.putAll(claims);
        return this;
    }

    TestCredentialBuilder withoutKeyId() {
        includeKeyId = false;
        return this;
    }

    TestCredentialBuilder withoutX5cHeader() {
        includeX5cHeader = false;
        return this;
    }

    String trustedIssuerCertificate() {
        return PemUtils.encodeCertificate(certificateChain(issuerKey).get(0));
    }

    String issuerPrivateKey() {
        return PemUtils.encodeKey(issuerKey.getPrivateKey());
    }

    String issuerAlgorithm() {
        return issuerKey.getAlgorithm();
    }

    String issuerKeyId() {
        return issuerKey.getKid();
    }

    TestIssuerMetadata jwtVcIssuerMetadata() throws Exception {
        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[]{
                JWKBuilder.create()
                        .kid(issuerKey.getKid())
                        .algorithm(issuerKey.getAlgorithm())
                        .rsa(issuerKey.getPublicKey(), certificateChain(issuerKey))
        });

        ObjectNode metadata = JsonSerialization.mapper.createObjectNode();
        metadata.put("issuer", issuer);
        metadata.set("jwks", JsonSerialization.mapper.valueToTree(jwks));
        return new TestIssuerMetadata(JsonSerialization.mapper.writeValueAsString(metadata));
    }

    SdJwt build(AuthorizationRequest authorizationRequest) throws Exception {
        KeyWrapper holderKey = createHolderKey();
        SdJwtCredentialBody credentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential(), credentialBuildConfig());
        credentialBody.addKeyBinding(JWKBuilder.create().ec(holderKey.getPublicKey()));
        if (includeKeyId) {
            credentialBody.getIssuerSignedJWT().getJwsHeader().setKeyId(issuerKey.getKid());
        }
        if (includeX5cHeader) {
            credentialBody.getIssuerSignedJWT().getJwsHeader().setX5c(certificateChain(issuerKey).stream()
                    .map(TestCredentialBuilder::encodeCertificate)
                    .toList());
        }

        KeyBindingJWT keyBindingJWT = KeyBindingJWT.builder()
                .withIat(Time.currentTime())
                .withAudience(authorizationRequest.getClientId())
                .withNonce(authorizationRequest.getNonce())
                .build();

        return SdJwt.builder()
                .withIssuerSignedJwt(credentialBody.getIssuerSignedJWT())
                .withKeybindingJwt(keyBindingJWT)
                .withIssuerSigningContext(KeyWrapperUtil.createSignatureSignerContext(issuerKey))
                .withKeyBindingSigningContext(KeyWrapperUtil.createSignatureSignerContext(holderKey))
                .withUseDefaultDecoys(false)
                .build();
    }

    private CredentialBuildConfig credentialBuildConfig() {
        return new CredentialBuildConfig()
                .setCredentialIssuer(issuer)
                .setCredentialType(credentialType)
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(0)
                .setSdJwtVisibleClaims(VISIBLE_CLAIMS);
    }

    private VerifiableCredential testCredential() {
        CredentialSubject credentialSubject = new CredentialSubject();
        claims.forEach(credentialSubject::setClaims);

        VerifiableCredential credential = new VerifiableCredential();
        credential.setId(URI.create("urn:uuid:" + UUID.randomUUID()));
        credential.setIssuer(URI.create(issuer));
        credential.setIssuanceDate(Instant.ofEpochSecond(Time.currentTime() - 60));
        credential.setExpirationDate(Instant.ofEpochSecond(Time.currentTime() + 600));
        credential.setCredentialSubject(credentialSubject);
        return credential;
    }

    private static List<X509Certificate> certificateChain(KeyWrapper key) {
        if (key.getCertificateChain() != null && !key.getCertificateChain().isEmpty()) {
            return key.getCertificateChain();
        }
        if (key.getCertificate() != null) {
            return List.of(key.getCertificate());
        }
        throw new IllegalStateException("Missing signing certificate chain");
    }

    private static String encodeCertificate(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyWrapper createHolderKey() {
        KeyWrapper key = OID4VCProofTestUtils.createEcKeyPair("holder-" + UUID.randomUUID());
        key.setUse(KeyUse.SIG);
        key.setAlgorithm(Algorithm.ES256);
        return key;
    }

}
