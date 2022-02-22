package org.keycloak.social.cognito;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class AwsCognitoOIDCIdentityProvider extends OIDCIdentityProvider implements SocialIdentityProvider<OIDCIdentityProviderConfig> {
    public AwsCognitoOIDCIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
        super(session, config);

        String defaultScope = config.getDefaultScope();

        if (!defaultScope.contains(SCOPE_OPENID)) {
            config.setDefaultScope((SCOPE_OPENID + " " + defaultScope).trim());
        }

    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {

        if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().equals("")) return null;
        String idToken = getIDTokenForLogout(session, userSession);
        if (idToken != null && getConfig().isBackchannelSupported()) {
            backchannelLogout(userSession, idToken);
            return null;
        } else {
            UriBuilder logoutUri = UriBuilder.fromUri(getConfig().getLogoutUrl());
            String redirectURI = uriInfo.getQueryParameters().get("redirect_uri").get(0).replace("#/", "");
            logoutUri.queryParam("logout_uri", redirectURI);
            logger.info("redirect Uri: " + redirectURI);
            AuthenticationManager.finishBrowserLogout(session, realm, userSession, session.getContext().getUri(), session.getContext().getConnection(), session.getContext().getRequestHeaders());
            Response response = Response.status(302).location(logoutUri.build()).build();
            return response;
        }
    }

    private String getIDTokenForLogout(KeycloakSession session, UserSessionModel userSession) {
        String tokenExpirationString = userSession.getNote(FEDERATED_TOKEN_EXPIRATION);
        long exp = tokenExpirationString == null ? 0 : Long.parseLong(tokenExpirationString);
        int currentTime = Time.currentTime();
        if (exp > 0 && currentTime > exp) {
            String response = refreshTokenForLogout(session, userSession);
            AccessTokenResponse tokenResponse = null;
            try {
                tokenResponse = JsonSerialization.readValue(response, AccessTokenResponse.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return tokenResponse.getIdToken();
        } else {
            return userSession.getNote(FEDERATED_ID_TOKEN);
        }
    }
}