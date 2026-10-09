package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vpRequestObjectResponse extends AbstractHttpResponse {

    private String requestObject;

    public Oid4vpRequestObjectResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    @Override
    protected void parseContent() throws IOException {
        requestObject = asString();
    }

    // The compact JWS of the signed authorization request object
    public String getRequestObject() {
        if (requestObject == null) {
            throw new IllegalStateException(String.format("[%s] %s", getError(), getErrorDescription()));
        }
        return requestObject;
    }

    // The OID4VP authorization request parameters
    public JsonNode getClaims() {
        try {
            return JsonSerialization.readValue(new JWSInput(getRequestObject()).getContent(), JsonNode.class);
        } catch (IOException | JWSInputException e) {
            throw new IllegalStateException("Failed to parse the request object", e);
        }
    }
}
