package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class AccessTokenRequest extends AbstractHttpPostRequest<AccessTokenRequest, AccessTokenResponse> {

    private final String code;
    private boolean omitRedirectUri = false;
    private boolean omitClientId = false;
    private String redirectUriOverride = null;

    AccessTokenRequest(String code, AbstractOAuthClient<?> client) {
        super(client);
        this.code = code;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public AccessTokenRequest signedJwt(String signedJwt) {
        parameter(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
        parameter(OAuth2Constants.CLIENT_ASSERTION, signedJwt);
        return this;
    }

    public AccessTokenRequest codeVerifier(PkceGenerator pkceGenerator) {
        if (pkceGenerator != null) {
            codeVerifier(pkceGenerator.getCodeVerifier());
        }
        return this;
    }

    public AccessTokenRequest codeVerifier(String codeVerifier) {
        parameter(OAuth2Constants.CODE_VERIFIER, codeVerifier);
        return this;
    }

    public AccessTokenRequest authorizationDetails(List<OID4VCAuthorizationDetail> authDetails) {
        parameter(OAuth2Constants.AUTHORIZATION_DETAILS, JsonSerialization.valueAsString(authDetails));
        return this;
    }

    public AccessTokenRequest dpopProof(String dpopProof) {
        header(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        return this;
    }

    public AccessTokenRequest param(String name, String value) {
        parameter(name, value);
        return this;
    }

    /**
     * Omit the redirect_uri parameter from the token request.
     * This is useful for testing edge cases where redirect_uri should not be included.
     *
     * @return this request instance for method chaining
     */
    public AccessTokenRequest omitRedirectUri() {
        this.omitRedirectUri = true;
        return this;
    }

    /**
     * Override the redirect_uri parameter with a custom value.
     * This is useful for testing redirect_uri mismatch scenarios.
     *
     * @param redirectUri the custom redirect URI to use
     * @return this request instance for method chaining
     */
    public AccessTokenRequest redirectUri(String redirectUri) {
        this.redirectUriOverride = redirectUri;
        return this;
    }

    /**
     * Omit the client_id parameter from the token request.
     * This is useful for testing edge cases where client_id should not be included.
     *
     * @return this request instance for method chaining
     */
    public AccessTokenRequest omitClientId() {
        this.omitClientId = true;
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE);

        parameter(OAuth2Constants.CODE, code);
        
        if (!omitRedirectUri) {
            if (redirectUriOverride != null) {
                parameter(OAuth2Constants.REDIRECT_URI, redirectUriOverride);
            } else {
                parameter(OAuth2Constants.REDIRECT_URI, client.getRedirectUri());
            }
        }
    }

    @Override
    protected void authorization() {
        if (omitClientId) {
            // Skip client_id parameter entirely
            // If client_secret is provided, add it as a parameter (not in Authorization header)
            // This is an edge case for testing invalid requests
            if (clientSecret != null) {
                parameter(OAuth2Constants.CLIENT_SECRET, clientSecret);
            }
        } else {
            super.authorization();
        }
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
