package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.Optional;

import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vcCredentialResponse extends AbstractHttpResponse {

    private CredentialResponse credentialResponse;
    private String encryptedCredentialResponse;

    public Oid4vcCredentialResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        String contentType = getContentType();
        if (contentType != null && contentType.startsWith("application/jwt")) {
            encryptedCredentialResponse = asString();
        } else {
            credentialResponse = asJson(CredentialResponse.class);
        }
    }

    public CredentialResponse getCredentialResponse() {
        return Optional.ofNullable(credentialResponse).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }

    public String getEncryptedCredentialResponse() {
        return Optional.ofNullable(encryptedCredentialResponse).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }

    public CredentialResponse getCredentialResponse(PrivateKey privateKey) {
        try {
            JWE jwe = new JWE(getEncryptedCredentialResponse());
            jwe.getKeyStorage().setDecryptionKey(privateKey);
            jwe.verifyAndDecodeJwe();
            return JsonSerialization.readValue(jwe.getContent(), CredentialResponse.class);
        } catch (IOException | JWEException e) {
            throw new IllegalStateException("Failed to decrypt credential response", e);
        }
    }
}
