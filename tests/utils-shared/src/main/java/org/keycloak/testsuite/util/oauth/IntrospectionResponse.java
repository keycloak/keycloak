package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class IntrospectionResponse extends AbstractHttpResponse {

    private String raw;

    IntrospectionResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        raw = asString();
    }

    public String getRaw() {
        return raw;
    }

    public JsonNode asJsonNode() throws IOException {
        return JsonSerialization.readValue(raw, JsonNode.class);
    }

    public TokenMetadataRepresentation asTokenMetadata() throws IOException {
        return JsonSerialization.readValue(raw, TokenMetadataRepresentation.class);
    }

}
