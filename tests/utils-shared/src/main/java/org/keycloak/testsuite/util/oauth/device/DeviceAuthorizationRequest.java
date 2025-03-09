package org.keycloak.testsuite.util.oauth.device;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;

import java.io.IOException;

public class DeviceAuthorizationRequest extends AbstractHttpPostRequest<DeviceAuthorizationRequest, DeviceAuthorizationResponse> {

    DeviceAuthorizationRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getDeviceAuthorization();
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.SCOPE, client.config().getScope());
        parameter(OIDCLoginProtocol.NONCE_PARAM, client.getNonce());
        parameter(OAuth2Constants.CODE_CHALLENGE, client.getCodeChallenge());
        parameter(OAuth2Constants.CODE_CHALLENGE_METHOD, client.getCodeChallengeMethod());
    }

    @Override
    protected DeviceAuthorizationResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new DeviceAuthorizationResponse(response);
    }

}
