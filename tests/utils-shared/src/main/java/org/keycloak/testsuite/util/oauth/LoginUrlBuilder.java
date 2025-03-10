package org.keycloak.testsuite.util.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

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

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.RESPONSE_TYPE, client.config().getResponseType());
        parameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM, client.config().getResponseMode());
        parameter(OAuth2Constants.CLIENT_ID, client.config().getClientId());
        parameter(OAuth2Constants.REDIRECT_URI, client.config().getRedirectUri());

        parameter(OAuth2Constants.STATE, client.getState());
        parameter(OIDCLoginProtocol.NONCE_PARAM, client.getNonce());
        parameter(OAuth2Constants.SCOPE, client.config().getScope());

        parameter(OAuth2Constants.CODE_CHALLENGE, client.getCodeChallenge());
        parameter(OAuth2Constants.CODE_CHALLENGE_METHOD, client.getCodeChallengeMethod());

        parameter(OIDCLoginProtocol.DPOP_JKT, client.getDpopJkt());

        parameter(OIDCLoginProtocol.REQUEST_PARAM, client.getRequest());
        parameter(OIDCLoginProtocol.REQUEST_URI_PARAM, client.getRequestUri());
        parameter(OIDCLoginProtocol.CLAIMS_PARAM, client.getClaims());

        if (client.getCustomParameters() != null) {
            client.getCustomParameters().forEach(this::parameter);
        }
    }

}
