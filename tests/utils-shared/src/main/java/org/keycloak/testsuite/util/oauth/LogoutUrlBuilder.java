package org.keycloak.testsuite.util.oauth;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;

public class LogoutUrlBuilder extends AbstractUrlBuilder {

    LogoutUrlBuilder(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    public String getEndpoint() {
        return client.getEndpoints().getLogout();
    }

    public LogoutUrlBuilder param(String name, String value) {
        parameter(name, value);
        return this;
    }

    public LogoutUrlBuilder idTokenHint(String idTokenHint) {
        parameter(OIDCLoginProtocol.ID_TOKEN_HINT, idTokenHint);
        return this;
    }

    public LogoutUrlBuilder postLogoutRedirectUri(String redirectUri) {
        parameter(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, redirectUri);
        return this;
    }

    public LogoutUrlBuilder state(String state) {
        parameter(OIDCLoginProtocol.STATE_PARAM, state);
        return this;
    }

    public LogoutUrlBuilder uiLocales(String uiLocales) {
        parameter(OIDCLoginProtocol.UI_LOCALES_PARAM, uiLocales);
        return this;
    }

    public LogoutUrlBuilder initiatingIdp(String initiatingIdp) {
        parameter(AuthenticationManager.INITIATING_IDP_PARAM, initiatingIdp);
        return this;
    }

    public LogoutUrlBuilder withClientId() {
        parameter(OIDCLoginProtocol.CLIENT_ID_PARAM, client.config().getClientId());
        return this;
    }

    public LogoutUrlBuilder withRedirect() {
        postLogoutRedirectUri(client.config().getPostLogoutRedirectUri());
        return this;
    }

    @Override
    protected void initRequest() {
    }

}
