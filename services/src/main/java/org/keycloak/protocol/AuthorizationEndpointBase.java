package org.keycloak.protocol;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.LoginActionsService;

/**
 * Common base class for Authorization REST endpoints implementation, which have to be implemented by each protocol.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class AuthorizationEndpointBase {

    private static final Logger logger = Logger.getLogger(AuthorizationEndpointBase.class);

    protected RealmModel realm;
    protected EventBuilder event;
    protected AuthenticationManager authManager;

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected HttpRequest request;
    @Context
    protected KeycloakSession session;
    @Context
    protected ClientConnection clientConnection;

    public AuthorizationEndpointBase(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        this.realm = realm;
        this.event = event;
        this.authManager = authManager;
    }

    protected AuthenticationProcessor createProcessor(ClientSessionModel clientSession, String flowId, String flowPath) {
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setClientSession(clientSession)
                .setFlowPath(flowPath)
                .setFlowId(flowId)
                .setBrowserFlow(true)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setProtector(authManager.getProtector())
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);
        return processor;
    }

    /**
     * Common method to handle browser authentication request in protocols unified way.
     * 
     * @param clientSession for current request
     * @param protocol handler for protocol used to initiate login
     * @param isPassive set to true if login should be passive (without login screen shown)
     * @return response to be returned to the browser
     */
    protected Response handleBrowserAuthenticationRequest(ClientSessionModel clientSession, LoginProtocol protocol, boolean isPassive) {

        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.isAuthenticateByDefault()) {
                // TODO if we are isPassive we should propagate this flag to default identity provider also if possible
                return buildRedirectToIdentityProvider(identityProvider.getAlias(), new ClientSessionCode(realm, clientSession).getCode());
            }
        }

        AuthenticationFlowModel flow = realm.getBrowserFlow();
        String flowId = flow.getId();
        AuthenticationProcessor processor = createProcessor(clientSession, flowId, LoginActionsService.AUTHENTICATE_PATH);

        if (isPassive) {
            // OIDC prompt == NONE or SAML 2 IsPassive flag
            // This means that client is just checking if the user is already completely logged in.
            // We cancel login if any authentication action or required action is required
            Response challenge = null;
            Response challenge2 = null;
            try {
                challenge = processor.authenticateOnly();
                if (challenge == null) {
                    challenge2 = processor.attachSessionExecutionRequiredActions();
                }
            } catch (Exception e) {
                return processor.handleBrowserException(e);
            }

            if (challenge != null || challenge2 != null) {
                if (processor.isUserSessionCreated()) {
                    session.sessions().removeUserSession(realm, processor.getUserSession());
                }
                if (challenge != null)
                    return protocol.sendError(clientSession, Error.PASSIVE_LOGIN_REQUIRED);
                else
                    return protocol.sendError(clientSession, Error.PASSIVE_INTERACTION_REQUIRED);
            } else {
                return processor.finishAuthentication();
            }
        } else {
            try {
                RestartLoginCookie.setRestartCookie(realm, clientConnection, uriInfo, clientSession);
                return processor.authenticate();
            } catch (Exception e) {
                return processor.handleBrowserException(e);
            }
        }
    }

    protected Response buildRedirectToIdentityProvider(String providerId, String accessCode) {
        logger.debug("Automatically redirect to identity provider: " + providerId);
        return Response.temporaryRedirect(
                Urls.identityProviderAuthnRequest(this.uriInfo.getBaseUri(), providerId, this.realm.getName(), accessCode))
                .build();
    }

}