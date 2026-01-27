package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.client.methods.CloseableHttpResponse;

public class PreAuthorizedCodeGrantRequest extends AbstractHttpPostRequest<PreAuthorizedCodeGrantRequest, AccessTokenResponse> {

    private final String preAuthorizedCode;

    public PreAuthorizedCodeGrantRequest(String preAuthorizedCode, AbstractOAuthClient<?> client) {
        super(client);
        this.preAuthorizedCode = preAuthorizedCode;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE);
        parameter(PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, preAuthorizedCode);
    }

    /**
     * Add a custom parameter to the token request.
     * This is useful for adding authorization_details or other custom parameters.
     */
    public PreAuthorizedCodeGrantRequest addParameter(String name, String value) {
        parameter(name, value);
        return this;
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }
}
