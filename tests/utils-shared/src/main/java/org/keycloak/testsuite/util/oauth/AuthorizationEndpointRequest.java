package org.keycloak.testsuite.util.oauth;

import java.util.List;

import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;

public class AuthorizationEndpointRequest {

    protected final AbstractOAuthClient<?> client;
    protected final LoginUrlBuilder loginForm;

    public AuthorizationEndpointRequest(AbstractOAuthClient<?> client) {
        this.client = client;
        this.loginForm = client.loginForm();
    }

    public AuthorizationEndpointRequest authorizationDetails(AuthorizationDetailsJSONRepresentation authDetail) {
        loginForm.authorizationDetails(List.of(authDetail));
        return this;
    }

    public AuthorizationEndpointRequest authorizationDetails(List<AuthorizationDetailsJSONRepresentation> authDetails) {
        loginForm.authorizationDetails(authDetails);
        return this;
    }

    public AuthorizationEndpointRequest client(String clientId) {
        loginForm.clientId(clientId);
        return this;
    }

    public AuthorizationEndpointRequest clientState(String clientState) {
        loginForm.state(clientState);
        return this;
    }

    public AuthorizationEndpointRequest codeChallenge(PkceGenerator pkce) {
        loginForm.codeChallenge(pkce);
        return this;
    }

    public AuthorizationEndpointRequest redirectUri(String redirectUri) {
        loginForm.redirectUri(redirectUri);
        return this;
    }

    public AuthorizationEndpointRequest responseType(String responseType) {
        loginForm.responseType(responseType);
        return this;
    }

    public AuthorizationEndpointRequest responseMode(String responseMode) {
        loginForm.responseMode(responseMode);
        return this;
    }

    public AuthorizationEndpointRequest request(String request) {
        loginForm.request(request);
        return this;
    }

    public AuthorizationEndpointRequest scope(String... scopes) {
        loginForm.scope(scopes);
        return this;
    }

    public AuthorizationEndpointRequest nonce(String nonce) {
        loginForm.nonce(nonce);
        return this;
    }

    protected String getEndpoint() {
        return client.getEndpoints().getAuthorization();
    }

    protected void initRequest() {
    }

    public AuthorizationEndpointResponse send(String username, String password) {
        initRequest();
        loginForm.open();
        client.fillLoginForm(username, password);
        return client.parseLoginResponse();
    }
}
