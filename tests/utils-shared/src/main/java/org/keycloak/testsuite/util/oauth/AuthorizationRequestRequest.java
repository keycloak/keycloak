package org.keycloak.testsuite.util.oauth;

import java.util.Arrays;

import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.utils.PkceGenerator;
import org.keycloak.util.Strings;

import org.apache.http.client.methods.CloseableHttpResponse;

import static org.keycloak.OAuth2Constants.SCOPE_OPENID;

public class AuthorizationRequestRequest extends AbstractHttpGetRequest<AuthorizationRequestRequest, AuthorizationRequestResponse> {

    private final AuthorizationRequest authRequest;
    private String username;
    private String password;

    public AuthorizationRequestRequest(AbstractOAuthClient<?> client, AuthorizationRequest authRequest) {
        super(client);
        if (authRequest.getClientId() == null) {
            authRequest.setClientId(client.getClientId());
        }
        if (authRequest.getRedirectUri() == null) {
            authRequest.setRedirectUri(client.getRedirectUri());
        }
        this.authRequest = authRequest;
    }

    public AuthorizationRequestRequest authorizationDetail(OID4VCAuthorizationDetail... authDetail) {
        authRequest.setAuthorizationDetails(authDetail != null ? Arrays.asList(authDetail) : null);
        return this;
    }

    public AuthorizationRequestRequest clientId(String clientId) {
        authRequest.setClientId(clientId);
        return this;
    }

    public AuthorizationRequestRequest clientState(String clientState) {
        authRequest.setState(clientState);
        return this;
    }

    public AuthorizationRequestRequest issuerState(String issuerState) {
        authRequest.setIssuerState(issuerState);
        return this;
    }

    public AuthorizationRequestRequest codeChallenge(PkceGenerator pkce) {
        authRequest.setCodeChallenge(pkce.getCodeChallenge());
        authRequest.setCodeChallengeMethod(pkce.getCodeChallengeMethod());
        return this;
    }

    public AuthorizationRequestRequest redirectUri(String redirectUri) {
        authRequest.setRedirectUri(redirectUri);
        return this;
    }

    public AuthorizationRequestRequest responseType(String responseType) {
        authRequest.setResponseType(responseType);
        return this;
    }

    public AuthorizationRequestRequest responseMode(String responseMode) {
        authRequest.setResponseMode(responseMode);
        return this;
    }

    public AuthorizationRequestRequest responseUri(String responseUri) {
        authRequest.setResponseUri(responseUri);
        return this;
    }

    public AuthorizationRequestRequest request(String request) {
        authRequest.setRequest(request);
        return this;
    }

    public AuthorizationRequestRequest scope(String... scope) {
        authRequest.setScope(String.join(" ", scope));
        return this;
    }

    public AuthorizationRequestRequest nonce(String nonce) {
        authRequest.setNonce(nonce);
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getAuthorization();
    }

    @Override
    protected void initRequest() {

        if (authRequest.getResponseType() == null)
            authRequest.setResponseType("code");

        // direct_post response mode requires response_uri
        if ("direct_post".equals(authRequest.getResponseMode())) {
            if (authRequest.getRedirectUri() != null)
                throw new IllegalStateException("redirect_uri must be null for direct_post");
            if (authRequest.getResponseUri() == null)
                throw new IllegalStateException("response_uri required for direct_post");
        }

        // Default scope openid if none provided
        if (Strings.isEmpty(authRequest.getScope()))
            authRequest.setScope(SCOPE_OPENID);
    }

    public AuthorizationRequestResponse send(String username, String password) {
        this.username = username;
        this.password = password;
        return send();
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
