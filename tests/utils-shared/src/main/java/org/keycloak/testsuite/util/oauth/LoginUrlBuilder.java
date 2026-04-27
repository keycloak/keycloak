package org.keycloak.testsuite.util.oauth;

import java.util.Arrays;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.ClaimsRepresentation;

public class LoginUrlBuilder extends AbstractUrlBuilder {

    public LoginUrlBuilder(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    public String getEndpoint() {
        return client.getEndpoints().getAuthorization();
    }

    public LoginUrlBuilder param(String name, String value) {
        parameter(name, value);
        return this;
    }

    public LoginUrlBuilder scope(String... scopes) {
        String joinedScopes = String.join(" ", Arrays.asList(scopes));
        parameter(OAuth2Constants.SCOPE, joinedScopes);
        return this;
    }

    public LoginUrlBuilder resource(String resource) {
        parameter(OAuth2Constants.RESOURCE, resource);
        return this;
    }

    public LoginUrlBuilder authorizationDetails(AuthorizationDetailsJSONRepresentation authDetail) {
        parameter(OAuth2Constants.AUTHORIZATION_DETAILS, authDetail != null ? List.of(authDetail) : List.of());
        return this;
    }

    public LoginUrlBuilder authorizationDetails(List<AuthorizationDetailsJSONRepresentation> authDetails) {
        parameter(OAuth2Constants.AUTHORIZATION_DETAILS, authDetails);
        return this;
    }

    public LoginUrlBuilder state(String state) {
        parameter(OIDCLoginProtocol.STATE_PARAM, state);
        return this;
    }

    public LoginUrlBuilder issuerState(String issuerState) {
        parameter(OAuth2Constants.ISSUER_STATE, issuerState);
        return this;
    }

    public LoginUrlBuilder nonce(String nonce) {
        parameter(OIDCLoginProtocol.NONCE_PARAM, nonce);
        return this;
    }

    public LoginUrlBuilder prompt(String prompt) {
        parameter(OIDCLoginProtocol.PROMPT_PARAM, prompt);
        return this;
    }

    public LoginUrlBuilder loginHint(String loginHint) {
        parameter(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        return this;
    }

    public LoginUrlBuilder uiLocales(String uiLocales) {
        parameter(OAuth2Constants.UI_LOCALES_PARAM, uiLocales);
        return this;
    }

    public LoginUrlBuilder maxAge(int maxAge) {
        parameter(OIDCLoginProtocol.MAX_AGE_PARAM, Integer.toString(maxAge));
        return this;
    }

    public LoginUrlBuilder kcAction(String kcAction) {
        parameter(Constants.KC_ACTION, kcAction);
        return this;
    }

    public LoginUrlBuilder codeChallenge(PkceGenerator pkceGenerator) {
        if (pkceGenerator != null) {
            codeChallenge(pkceGenerator.getCodeChallenge(), pkceGenerator.getCodeChallengeMethod());
        }
        return this;
    }

    public LoginUrlBuilder codeChallenge(String codeChallenge, String codeChallengeMethod) {
        parameter(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
        parameter(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        return this;
    }

    public LoginUrlBuilder dpopJkt(String dpopJkt) {
        parameter(OIDCLoginProtocol.DPOP_JKT, dpopJkt);
        return this;
    }

    public LoginUrlBuilder claims(ClaimsRepresentation claims) {
        parameter(OIDCLoginProtocol.CLAIMS_PARAM, claims);
        return this;
    }

    public LoginUrlBuilder request(String request) {
        parameter(OIDCLoginProtocol.REQUEST_PARAM, request);
        return this;
    }

    public LoginUrlBuilder requestUri(String requestUri) {
        parameter(OIDCLoginProtocol.REQUEST_URI_PARAM, requestUri);
        return this;
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.RESPONSE_TYPE, client.config().getResponseType());
        parameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM, client.config().getResponseMode());
        parameter(OAuth2Constants.CLIENT_ID, client.config().getClientId());
        parameter(OAuth2Constants.REDIRECT_URI, client.config().getRedirectUri());
        if (!params.containsKey(OAuth2Constants.SCOPE)) {
            parameter(OAuth2Constants.SCOPE, client.config().getScope());
        }
    }

    /**
     * Composite login method for the Authorization Code Flow
     *
     * <ol>
     *  <li>It builds and opens the authorization request url</li>
     *  <li>Fills the login form with user credentials (i.e. username, password)</li>
     *  <li>Parses the authorization response</li>
     * </ol>
     *
     * This method is intended to be used only for the purpose of the basic login flow when the server is expected to open a login form.
     *
     * For more complex scenarios like:
     * <ul>
     *     <li>SSO login (user being automatically authenticated without the need to provide a username/password</li>
     *     <li>Automatic redirect to the client with the error as result of an invalid authorization request</li>
     *     <li>The call not being redirected back to the client either due to an incorrect username/password or some other screen being displayed</li>
     * </ul>
     *
     * calls to level API will be needed.
     *
     * In short, the caller should always know whether they expect a login-form to be shown or not.
     * For details, there is <a href="https://github.com/keycloak/keycloak/discussions/48308">this discussion</a>.
     */
    public AuthorizationEndpointResponse doLogin(String username, String password) {
        open();
        client.fillLoginForm(username, password);
        return client.parseLoginResponse();
    }

    public AuthorizationEndpointResponse doLoginWithCookie() {
        open();
        return client.parseLoginResponse();
    }

}
