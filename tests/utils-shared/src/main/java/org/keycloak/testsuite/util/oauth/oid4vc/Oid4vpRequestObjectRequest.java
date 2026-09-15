package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vpRequestObjectRequest extends AbstractHttpGetRequest<Oid4vpRequestObjectRequest, Oid4vpRequestObjectResponse> {

    private final String requestUri;

    public Oid4vpRequestObjectRequest(AbstractOAuthClient<?> client, String requestUri) {
        super(client);
        this.requestUri = requestUri;
    }

    @Override
    protected String getEndpoint() {
        return requestUri;
    }

    @Override
    protected void initRequest() {
        header("Accept", "application/oauth-authz-req+jwt");
    }

    @Override
    protected Oid4vpRequestObjectResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new Oid4vpRequestObjectResponse(response);
    }
}
