package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

public class Oid4vcCredentialRequest extends AbstractHttpPostRequest<Oid4vcCredentialRequest, Oid4vcCredentialResponse> {

    protected final CredentialRequest credRequest;
    private String rawPayload;
    private ContentType payloadContentType = ContentType.APPLICATION_JSON;

    public Oid4vcCredentialRequest(AbstractOAuthClient<?> client, CredentialRequest credRequest) {
        super(client);
        this.credRequest = credRequest;
    }

    public Oid4vcCredentialRequest credentialConfigurationId(String credentialConfigurationId) {
        credRequest.setCredentialConfigurationId(credentialConfigurationId);
        return this;
    }

    public Oid4vcCredentialRequest credentialIdentifier(String credentialIdentifier) {
        credRequest.setCredentialIdentifier(credentialIdentifier);
        return this;
    }

    public Oid4vcCredentialRequest proofs(Proofs proofs) {
        credRequest.setProofs(proofs);
        return this;
    }

    public CredentialRequest getCredentialRequest() {
        return credRequest;
    }

    /**
     * Override outgoing payload and content type.
     */
    public Oid4vcCredentialRequest payload(String payload, ContentType contentType) {
        this.rawPayload = payload;
        this.payloadContentType = contentType;
        return this;
    }

    /**
     * Encrypt current credential request as compact JWE and send with application/jwt.
     */
    public Oid4vcCredentialRequest encryptRequest(JWK issuerEncryptionJwk, boolean useDeflateCompression) {
        try {
            String requestPayload = JsonSerialization.valueAsString(credRequest);
            String jwePayload = encryptPayload(requestPayload, issuerEncryptionJwk, useDeflateCompression);
            return payload(jwePayload, ContentType.create("application/jwt", StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credential request", e);
        }
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcCredential();
    }

    @Override
    protected void initRequest() {
        if (rawPayload != null) {
            entity = new StringEntity(rawPayload, payloadContentType);
        } else if (credRequest != null) {
            String payload = JsonSerialization.valueAsString(credRequest);
            entity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        } else {
            // Trigger an empty payload in {@link AbstractHttpPostRequest#send()}.
            entity = new StringEntity("", ContentType.APPLICATION_JSON);
        }
    }

    @Override
    protected Oid4vcCredentialResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new Oid4vcCredentialResponse(response);
    }

    private static String encryptPayload(String payload, JWK issuerEncJwk, boolean useCompression) throws Exception {
        PublicKey publicKey = JWKParser.create(issuerEncJwk).toPublicKey();
        JWEHeader.JWEHeaderBuilder builder = new JWEHeader.JWEHeaderBuilder()
                .keyId(issuerEncJwk.getKeyId())
                .algorithm(issuerEncJwk.getAlgorithm())
                .encryptionAlgorithm("A256GCM")
                .type("JWT");
        if (useCompression) {
            builder.compressionAlgorithm("DEF");
        }
        byte[] content = useCompression ? compressPayload(payload.getBytes(StandardCharsets.UTF_8))
                : payload.getBytes(StandardCharsets.UTF_8);
        JWE jwe = new JWE().header(builder.build()).content(content);
        jwe.getKeyStorage().setEncryptionKey(publicKey);
        return jwe.encodeJwe();
    }

    private static byte[] compressPayload(byte[] payload) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true))) {
            deflater.write(payload);
        }
        return out.toByteArray();
    }
}
