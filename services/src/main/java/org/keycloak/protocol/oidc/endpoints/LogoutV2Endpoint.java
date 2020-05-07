package org.keycloak.protocol.oidc.endpoints;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.LogoutTokenValidationCode;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.LogoutToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class LogoutV2Endpoint {

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    private TokenManager tokenManager;
    private RealmModel realm;
    private EventBuilder event;

    public LogoutV2Endpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
    }

    /**
     * Backchannel logout endpoint implementation for Keycloak, which tries to logout the user from all sessions via
     * POST with a valid LogoutToken.
     *
     * Logout a session via a non-browser invocation. Will be implemented as a backchannel logout based on the
     * specification
     * https://openid.net/specs/openid-connect-backchannel-1_0.html
     *
     * @return
     */
    @Path("/backchannel-logout")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response backchannelLogout() {
        MultivaluedMap<String, String> form = request.getDecodedFormParameters();
        event.event(EventType.LOGOUT);

        String encodedLogoutToken = form.getFirst(OAuth2Constants.LOGOUT_TOKEN);
        if (encodedLogoutToken == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "No logout token",
                    Response.Status.BAD_REQUEST);
        }

        LogoutTokenValidationCode validationCode = tokenManager.verifyLogoutToken(session, realm, encodedLogoutToken);
        if (!validationCode.equals(LogoutTokenValidationCode.VALIDATION_SUCCESS)) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, validationCode.getErrorMessage(),
                    Response.Status.BAD_REQUEST);
        }

        LogoutToken logoutToken = tokenManager.toLogoutToken(encodedLogoutToken).get();

        List<String> identityProviderAliases = tokenManager.getValidOIDCIdentityProvidersForBackchannelLogout(realm,
                session, encodedLogoutToken, logoutToken).stream()
                .map(idp -> idp.getConfig().getAlias())
                .collect(Collectors.toList());

        boolean localLogoutSucceeded;

        if (logoutToken.getSid() != null) {
            localLogoutSucceeded = backchannelLogoutWithSessionId(logoutToken.getSid(), identityProviderAliases);
        } else {
            localLogoutSucceeded = backchannelLogoutFederatedUserId(logoutToken.getSubject(), identityProviderAliases);
        }

        if (!localLogoutSucceeded) {
            event.error(Errors.LOGOUT_FAILED);
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR,
                    "There was an error in the local logout",
                    Response.Status.NOT_IMPLEMENTED);
        }

        session.getProvider(SecurityHeadersProvider.class).options().allowEmptyContentType();
        return Cors.add(request)
                .auth()
                .builder(Response.ok())
                .build();
    }

    private boolean backchannelLogoutWithSessionId(String sessionId, List<String> identityProviderAliases) {

        boolean allLogoutsSucceeded = true;

        for (String identityProviderAlias : identityProviderAliases) {
            UserSessionModel userSession = session.sessions().getUserSessionByBrokerSessionId(realm,
                    identityProviderAlias + "." + sessionId);

            if (userSession != null) {
                boolean logoutSucceeded = logoutUserSession(userSession);
                allLogoutsSucceeded = allLogoutsSucceeded && logoutSucceeded;
            }
        }

        return allLogoutsSucceeded;
    }

    private boolean backchannelLogoutFederatedUserId(String federatedUserId, List<String> identityProviderAliases) {

        boolean allLogoutsSucceeded = true;

        for (String identityProviderAlias : identityProviderAliases) {
            List<UserSessionModel> userSessions = session.sessions().getUserSessionByBrokerUserId(realm,
                    identityProviderAlias + "." + federatedUserId);

            for (UserSessionModel userSession : userSessions) {
                boolean logoutSucceeded = logoutUserSession(userSession);
                allLogoutsSucceeded = allLogoutsSucceeded && logoutSucceeded;
            }
        }

        return allLogoutsSucceeded;
    }

    private boolean logoutUserSession(UserSessionModel userSession) {
        boolean logoutSucceeded = AuthenticationManager.backchannelLogout(session, realm, userSession,
                session.getContext().getUri(),
                clientConnection, headers, false);

        if (logoutSucceeded) {
            event.user(userSession.getUser())
                    .session(userSession)
                    .success();
        }

        return logoutSucceeded;
    }
}