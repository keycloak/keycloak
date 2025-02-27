package org.keycloak.testsuite.util.oauth;

import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.managers.AuthenticationManager;

public class LogoutUrlBuilder {

    private final Endpoints endpoints;

    private String clientId;
    private String idTokenHint;
    private String redirectUri;
    private String state;
    private String uiLocales;
    private String initiatingIdp;

    LogoutUrlBuilder(Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    public LogoutUrlBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public LogoutUrlBuilder idTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
        return this;
    }

    public LogoutUrlBuilder postLogoutRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public LogoutUrlBuilder state(String state) {
        this.state = state;
        return this;
    }

    public LogoutUrlBuilder uiLocales(String uiLocales) {
        this.uiLocales = uiLocales;
        return this;
    }

    public LogoutUrlBuilder initiatingIdp(String initiatingIdp) {
        this.initiatingIdp = initiatingIdp;
        return this;
    }

    public String build() {
        UriBuilder b = OIDCLoginProtocolService.logoutUrl(endpoints.getBase());
        setNonNull(b, OIDCLoginProtocol.CLIENT_ID_PARAM, clientId);
        setNonNull(b, OIDCLoginProtocol.ID_TOKEN_HINT, idTokenHint);
        setNonNull(b, OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, redirectUri);
        setNonNull(b, OIDCLoginProtocol.STATE_PARAM, state);
        setNonNull(b, OIDCLoginProtocol.UI_LOCALES_PARAM, uiLocales);
        setNonNull(b, AuthenticationManager.INITIATING_IDP_PARAM, initiatingIdp);
        return endpoints.asString(b);
    }

    private void setNonNull(UriBuilder b, String name, String value) {
        if (value != null) {
            b.queryParam(name, value);
        }
    }

}
