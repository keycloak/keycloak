package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;
import org.keycloak.utils.MediaType;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialIssuerMetadataResponse extends AbstractHttpResponse {

    private CredentialIssuer metadata;
    private Object content;

    public CredentialIssuerMetadataResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        String contentType = getHeader(HttpHeaders.CONTENT_TYPE);
        if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JWT)) {
            try {
                JWSInput jwsInput = (JWSInput) (content = new JWSInput(asString()));
                metadata = JsonSerialization.readValue(jwsInput.getContent(), CredentialIssuer.class);
            } catch (JWSInputException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            String jsonInput = (String) (content = asString());
            metadata = JsonSerialization.valueFromString(jsonInput, CredentialIssuer.class);
        }
        // Sanity check that we have an 'issuer'
        if (Strings.isEmpty(metadata.getCredentialIssuer())) {
            throw new IllegalStateException("Invalid issuer metadata: " + content);
        }
    }

    public Object getContent() {
        return Optional.ofNullable(content).orElseThrow(() ->
                new IllegalStateException(getError()));
    }

    public CredentialIssuer getMetadata() {
        return Optional.ofNullable(metadata).orElseThrow(() ->
                new IllegalStateException(getError()));
    }
}
