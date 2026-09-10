package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.OID4VCConstants;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import org.apache.http.client.methods.CloseableHttpResponse;

public class Oid4vpDirectPostRequest extends AbstractHttpPostRequest<Oid4vpDirectPostRequest, Oid4vpDirectPostResponse> {

    private final String responseUri;
    private String vpToken;
    private String state;
    private String response;

    public Oid4vpDirectPostRequest(AbstractOAuthClient<?> client, String responseUri) {
        super(client);
        this.responseUri = responseUri;
    }

    public Oid4vpDirectPostRequest vpToken(String vpToken) {
        this.vpToken = vpToken;
        return this;
    }

    public Oid4vpDirectPostRequest state(String state) {
        this.state = state;
        return this;
    }

    // The encrypted direct_post.jwt response parameter (a compact JWE)
    public Oid4vpDirectPostRequest response(String response) {
        this.response = response;
        return this;
    }

    @Override
    protected String getEndpoint() {
        return responseUri;
    }

    @Override
    protected void initRequest() {
        parameter(OID4VCConstants.VP_TOKEN, vpToken);
        parameter(OAuth2Constants.STATE, state);
        parameter(OAuth2Constants.RESPONSE, response);
    }

    @Override
    protected Oid4vpDirectPostResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new Oid4vpDirectPostResponse(response);
    }
}
