package org.keycloak.protocol.oidc.endpoints;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LogoutEndpoint {

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    private TokenManager tokenManager;
    private AuthenticationManager authManager;
    private RealmModel realm;
    private EventBuilder event;

    public LogoutEndpoint(TokenManager tokenManager, AuthenticationManager authManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.authManager = authManager;
        this.realm = realm;
        this.event = event;
    }

    /**
     * Logout user session.  User must be logged in via a session cookie.
     *
     * @param redirectUri
     * @return
     */
    @GET
    @NoCache
    public Response logout(@QueryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM) String redirectUri) {
        if (redirectUri != null) {
            String validatedUri = RedirectUtils.verifyRealmRedirectUri(uriInfo, redirectUri, realm);
            if (validatedUri == null) {
                event.event(EventType.LOGOUT);
                event.detail(Details.REDIRECT_URI, redirectUri);
                event.error(Errors.INVALID_REDIRECT_URI);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.INVALID_REDIRECT_URI);
            }
            redirectUri = validatedUri;
        }

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, uriInfo, clientConnection, headers, false);
        if (authResult != null) {
            if (redirectUri != null) authResult.getSession().setNote(OIDCLoginProtocol.LOGOUT_REDIRECT_URI, redirectUri);
            authResult.getSession().setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, OIDCLoginProtocol.LOGIN_PROTOCOL);
            return AuthenticationManager.browserLogout(session, realm, authResult.getSession(), uriInfo, clientConnection, headers);
        }

        if (redirectUri != null) {
            return Response.status(302).location(UriBuilder.fromUri(redirectUri).build()).build();
        } else {
            return Response.ok().build();
        }
    }

    /**
     * Logout a session via a non-browser invocation.  Similar signature to refresh token except there is no grant_type.
     * You must pass in the refresh token and
     * authenticate the client if it is not public.
     *
     * If the client is a confidential client
     * you must include the client-id (application name or oauth client name) and secret in an Basic Auth Authorization header.
     *
     * If the client is a public client, then you must include a "client_id" form parameter with the app's or oauth client's name.
     *
     * returns 204 if successful, 400 if not with a json error response.
     *
     * @param authorizationHeader
     * @param form
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logoutToken(final @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                final MultivaluedMap<String, String> form) {
        checkSsl();

        event.event(EventType.LOGOUT);

        ClientModel client = authorizeClient(authorizationHeader, form, event);
        String refreshToken = form.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }
        try {
            RefreshToken token = tokenManager.verifyRefreshToken(realm, refreshToken);
            UserSessionModel userSessionModel = session.sessions().getUserSession(realm, token.getSessionState());
            if (userSessionModel != null) {
                logout(userSessionModel);
            }
        } catch (OAuthErrorException e) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
        }
        return Cors.add(request, Response.noContent()).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private void logout(UserSessionModel userSession) {
        authManager.logout(session, realm, userSession, uriInfo, clientConnection, headers);
        event.user(userSession.getUser()).session(userSession).success();
    }

    private ClientModel authorizeClient(String authorizationHeader, MultivaluedMap<String, String> formData, EventBuilder event) {
        ClientModel client = AuthorizeClientUtil.authorizeClient(authorizationHeader, formData, event, realm);

        if ( (client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
            throw new ErrorResponseException("invalid_client", "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }

        return client;
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException("invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

}
