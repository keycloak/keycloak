package org.keycloak.testsuite.util.oauth.device;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import org.apache.http.client.methods.CloseableHttpResponse;

public class DeviceAuthorizationRequest extends AbstractHttpPostRequest<DeviceAuthorizationRequest, DeviceAuthorizationResponse> {

    DeviceAuthorizationRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    public DeviceAuthorizationRequest codeChallenge(PkceGenerator pkceGenerator) {
        if (pkceGenerator != null) {
            codeChallenge(pkceGenerator.getCodeChallenge(), pkceGenerator.getCodeChallengeMethod());
        }
        return this;
    }

    public DeviceAuthorizationRequest codeChallenge(String codeChallenge, String codeChallengeMethod) {
        parameter(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
        parameter(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getDeviceAuthorization();
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.SCOPE, client.config().getScope());
    }

    @Override
    protected DeviceAuthorizationResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new DeviceAuthorizationResponse(response);
    }

}
