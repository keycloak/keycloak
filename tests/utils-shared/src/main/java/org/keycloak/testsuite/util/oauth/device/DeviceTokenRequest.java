package org.keycloak.testsuite.util.oauth.device;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.io.IOException;

public class DeviceTokenRequest extends AbstractHttpPostRequest<DeviceTokenRequest, AccessTokenResponse> {

    private final String deviceCode;

    DeviceTokenRequest(String deviceCode, AbstractOAuthClient<?> client) {
        super(client);
        this.deviceCode = deviceCode;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.DEVICE_CODE_GRANT_TYPE);
        parameter("device_code", deviceCode);
        parameter(OAuth2Constants.CODE_VERIFIER, client.getCodeVerifier());
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
