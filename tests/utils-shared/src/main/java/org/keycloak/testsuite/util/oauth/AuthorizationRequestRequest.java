package org.keycloak.testsuite.util.oauth;

import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;

import org.apache.http.client.methods.CloseableHttpResponse;

public class AuthorizationRequestRequest extends AbstractHttpGetRequest<AuthorizationRequestRequest, AuthorizationRequestResponse> {

    private final AuthorizationRequest authRequest;
    private String username;
    private String password;

    public AuthorizationRequestRequest(AbstractOAuthClient<?> client, AuthorizationRequest authRequest) {
        super(client);
        this.authRequest = authRequest;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getAuthorization();
    }

    @Override
    protected void initRequest() {
    }

    public AuthorizationRequestRequest credentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public AuthorizationRequestResponse send() {
        initRequest();
        String endpointUrl = authRequest.toRequestUrl(getEndpoint());
        client.driver.navigate().to(endpointUrl);
        client.fillLoginForm(username, password);
        return new AuthorizationRequestResponse(client);
    }

    @Override
    protected AuthorizationRequestResponse toResponse(CloseableHttpResponse response) {
        return null;
    }
}
