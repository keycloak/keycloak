package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class CredentialIssuerMetadataRequest extends AbstractHttpGetRequest<CredentialIssuerMetadataRequest, CredentialIssuerMetadataResponse> {

    public CredentialIssuerMetadataRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcIssuerMetadata();
    }

    @Override
    protected void initRequest() {
        // No specific parameters for metadata request
    }

    @Override
    protected CredentialIssuerMetadataResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new CredentialIssuerMetadataResponse(response);
    }
}
