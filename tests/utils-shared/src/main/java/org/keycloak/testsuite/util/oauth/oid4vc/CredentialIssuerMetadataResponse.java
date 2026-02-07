package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialIssuerMetadataResponse extends AbstractHttpResponse {

    private CredentialIssuer metadata;
    private String content;

    public CredentialIssuerMetadataResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        content = asString();
        String contentType = getHeader("Content-Type");
        if (contentType != null && contentType.startsWith("application/json")) {
            // Check if this is OID4VC metadata (has "credential_issuer") vs JWT VC metadata (has "issuer" only)
            // JWT VC metadata uses JWTVCIssuerMetadata model with "issuer" field
            // OID4VC metadata uses CredentialIssuer model with "credential_issuer" field
            // Only parse if it's OID4VC format - JWT VC endpoints return different format
            JsonNode node = JsonSerialization.readValue(content, JsonNode.class);
            if (node.has("credential_issuer")) {
                metadata = JsonSerialization.mapper.treeToValue(node, CredentialIssuer.class);
            }
        }
    }

    public String getContent() {
        return Optional.ofNullable(content).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }

    public CredentialIssuer getMetadata() {
        return Optional.ofNullable(metadata).orElseThrow(() ->
                new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription())));
    }
}
