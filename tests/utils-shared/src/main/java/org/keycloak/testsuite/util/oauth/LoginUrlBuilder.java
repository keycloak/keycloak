package org.keycloak.testsuite.util.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
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

    public LoginUrlBuilder state(String state) {
        parameter(OIDCLoginProtocol.STATE_PARAM, state);
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

        parameter(OAuth2Constants.SCOPE, client.config().getScope());
    }

    public AuthorizationEndpointResponse doLogin(String username, String password) {
        open();
        client.fillLoginForm(username, password);
        return client.parseLoginResponse();
    }

}
