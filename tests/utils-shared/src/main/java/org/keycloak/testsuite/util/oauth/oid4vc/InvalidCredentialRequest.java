package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/**
 * Request class for sending invalid credential requests for testing purposes.
 * This class extends AbstractOid4vcRequest to allow full control over the request body,
 * enabling tests to send requests with malformed JSON or missing required fields.
 * <p>
 * This should only be used in test cases that specifically test error handling
 * for invalid requests. For valid requests, use {@link Oid4vcCredentialRequest} instead.
 */
public class InvalidCredentialRequest extends AbstractHttpPostRequest<InvalidCredentialRequest, Oid4vcCredentialResponse> {

    private String bodyJson;

    public InvalidCredentialRequest(AbstractOAuthClient<?> client, String bodyJson) {
        super(client);
        this.bodyJson = bodyJson;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcCredential();
    }

    @Override
    protected void initRequest() {
        entity = new StringEntity(bodyJson, ContentType.APPLICATION_JSON);
    }

    @Override
    protected Oid4vcCredentialResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new Oid4vcCredentialResponse(response);
    }
}
