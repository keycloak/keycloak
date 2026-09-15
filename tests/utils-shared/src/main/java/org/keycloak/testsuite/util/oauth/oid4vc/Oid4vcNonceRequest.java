package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vcNonceRequest extends AbstractHttpPostRequest<Oid4vcNonceRequest, Oid4vcNonceResponse> {

    public Oid4vcNonceRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getOid4vcNonce();
    }

    @Override
    protected void initRequest() {
        // No parameters needed for nonce request
    }

    @Override
    protected Oid4vcNonceResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new Oid4vcNonceResponse(response);
    }
}
