package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;

public class PreAuthorizedCodeGrantRequest extends AbstractHttpPostRequest<PreAuthorizedCodeGrantRequest, AccessTokenResponse> {

    private final String preAuthorizedCode;

    PreAuthorizedCodeGrantRequest(AbstractOAuthClient<?> client, String preAuthorizedCode) {
        super(client);
        this.preAuthorizedCode = preAuthorizedCode;
    }

    public PreAuthorizedCodeGrantRequest authorizationDetails(List<OID4VCAuthorizationDetail> authDetails) {
        parameter(OAuth2Constants.AUTHORIZATION_DETAILS, JsonSerialization.valueAsString(authDetails));
        return this;
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

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }
}
