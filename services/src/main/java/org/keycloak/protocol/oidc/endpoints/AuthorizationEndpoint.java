package org.keycloak.protocol.oidc.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationEndpoint {

    private static final Logger logger = Logger.getLogger(AuthorizationEndpoint.class);
    public static final String CODE_AUTH_TYPE = "code";

    private enum Action {
        REGISTER, CODE
    }

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    private final AuthenticationManager authManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private ClientModel client;
    private ClientSessionModel clientSession;

    private Action action;

    private String clientId;
    private String redirectUri;
    private String redirectUriParam;
    private String responseType;
    private String state;
    private String scope;
    private String loginHint;
    private String prompt;
    private String nonce;
    private String idpHint;

    private String legacyResponseType;

    public AuthorizationEndpoint(AuthenticationManager authManager, RealmModel realm, EventBuilder event) {
        this.authManager = authManager;
        this.realm = realm;
        this.event = event;
        event.event(EventType.LOGIN);
    }

    @GET
    public Response build() {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

        clientId = params.getFirst(OIDCLoginProtocol.CLIENT_ID_PARAM);
        responseType = params.getFirst(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        redirectUriParam = params.getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        state = params.getFirst(OIDCLoginProtocol.STATE_PARAM);
        scope = params.getFirst(OIDCLoginProtocol.SCOPE_PARAM);
        loginHint = params.getFirst(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        prompt = params.getFirst(OIDCLoginProtocol.PROMPT_PARAM);
        idpHint = params.getFirst(AdapterConstants.KC_IDP_HINT);
        nonce = params.getFirst(OIDCLoginProtocol.NONCE_PARAM);

        checkSsl();
        checkRealm();
        checkClient();
        checkResponseType();
        checkRedirectUri();

        createClientSession();

        switch (action) {
            case REGISTER:
                return buildRegister();
            case CODE:
                return buildAuthorizationCodeAuthorizationResponse();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    /**
     * @deprecated
     */
    public AuthorizationEndpoint legacy(String legacyResponseType) {
        logger.warnv("Invoking deprecated endpoint {0}", uriInfo.getRequestUri());
        this.legacyResponseType = legacyResponseType;
        return this;
    }

    public AuthorizationEndpoint register() {
        event.event(EventType.REGISTER);
        action = Action.REGISTER;

        if (!realm.isRegistrationAllowed()) {
            throw new ErrorPageException(session, Messages.REGISTRATION_NOT_ALLOWED);
        }

        return this;
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            event.error(Errors.SSL_REQUIRED);
            throw new ErrorPageException(session, Messages.HTTPS_REQUIRED);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new ErrorPageException(session, Messages.REALM_NOT_ENABLED);
        }
    }

    private void checkClient() {
        if (clientId == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Messages.MISSING_PARAMETER, OIDCLoginProtocol.CLIENT_ID_PARAM );
        }

        event.client(clientId);

        client = realm.getClientByClientId(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, Messages.CLIENT_NOT_FOUND );
        }

        if (client.isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, Messages.BEARER_ONLY );
        }

        if (client.isDirectGrantsOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, Messages.DIRECT_GRANTS_ONLY);
        }

        session.getContext().setClient(client);
    }

    private void checkResponseType() {
        if (responseType == null) {
            if (legacyResponseType != null) {
                responseType = legacyResponseType;
            } else {
                event.error(Errors.INVALID_REQUEST);
                throw new ErrorPageException(session, Messages.MISSING_PARAMETER, OIDCLoginProtocol.RESPONSE_TYPE_PARAM );
            }
        }

        event.detail(Details.RESPONSE_TYPE, responseType);

        if (responseType.equals(OAuth2Constants.CODE)) {
            if (action == null) {
                action = Action.CODE;
            }
        } else {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Messages.INVALID_PARAMETER, OIDCLoginProtocol.RESPONSE_TYPE_PARAM );
        }
    }

    private void checkRedirectUri() {
        event.detail(Details.REDIRECT_URI, redirectUriParam);

        redirectUri = RedirectUtils.verifyRedirectUri(uriInfo, redirectUriParam, realm, client);
        if (redirectUri == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new ErrorPageException(session, Messages.INVALID_PARAMETER, OIDCLoginProtocol.REDIRECT_URI_PARAM);
        }
    }

    private void createClientSession() {
        clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirectUri);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        clientSession.setNote(ClientSessionCode.ACTION_KEY, KeycloakModelUtils.generateCodeSecret());
        clientSession.setNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, responseType);
        clientSession.setNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUriParam);
        clientSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));

        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        if (nonce != null) clientSession.setNote(OIDCLoginProtocol.NONCE_PARAM, nonce);
        if (scope != null) clientSession.setNote(OIDCLoginProtocol.SCOPE_PARAM, scope);
        if (loginHint != null) clientSession.setNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        if (prompt != null) clientSession.setNote(OIDCLoginProtocol.PROMPT_PARAM, prompt);
        if (idpHint != null) clientSession.setNote(AdapterConstants.KC_IDP_HINT, idpHint);
    }

    private Response buildAuthorizationCodeAuthorizationResponse() {
        String accessCode = new ClientSessionCode(realm, clientSession).getCode();

        if (idpHint != null && !"".equals(idpHint)) {
            IdentityProviderModel identityProviderModel = realm.getIdentityProviderByAlias(idpHint);

            if (identityProviderModel == null) {
                return session.getProvider(LoginFormsProvider.class)
                        .setError(Messages.IDENTITY_PROVIDER_NOT_FOUND, idpHint)
                        .createErrorPage();
            }
            return buildRedirectToIdentityProvider(idpHint, accessCode);
        }

        return browserAuthentication(accessCode);
    }

    protected Response browserAuthentication(String accessCode) {
        this.event.event(EventType.LOGIN);
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.isAuthenticateByDefault()) {
                return buildRedirectToIdentityProvider(identityProvider.getAlias(), accessCode);
            }
        }
        clientSession.setNote(Details.AUTH_TYPE, CODE_AUTH_TYPE);


        AuthenticationFlowModel flow = realm.getBrowserFlow();
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setClientSession(clientSession)
                .setFlowPath(LoginActionsService.AUTHENTICATE_PATH)
                .setFlowId(flowId)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setProtector(authManager.getProtector())
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(uriInfo)
                .setRequest(request);

        Response challenge = null;
        try {
            challenge = processor.authenticateOnly();
            if (challenge == null) {
                challenge = processor.attachSessionExecutionRequiredActions();
            }
        } catch (Exception e) {
            return processor.handleBrowserException(e);
        }

        if (challenge != null && prompt != null && prompt.equals("none")) {
            if (processor.isUserSessionCreated()) {
                session.sessions().removeUserSession(realm, processor.getUserSession());
            }
            OIDCLoginProtocol oauth = new OIDCLoginProtocol(session, realm, uriInfo, headers, event);
            return oauth.cancelLogin(clientSession);
        }

        if (challenge == null) {
            return processor.finishAuthentication();
        } else {
            RestartLoginCookie.setRestartCookie(realm, clientConnection, uriInfo, clientSession);
            return challenge;
        }
    }

    private Response buildRegister() {
        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        return session.getProvider(LoginFormsProvider.class)
                .setClientSessionCode(new ClientSessionCode(realm, clientSession).getCode())
                .createRegistration();
    }

    private Response buildRedirectToIdentityProvider(String providerId, String accessCode) {
        logger.debug("Automatically redirect to identity provider: " + providerId);
        return Response.temporaryRedirect(
                Urls.identityProviderAuthnRequest(this.uriInfo.getBaseUri(), providerId, this.realm.getName(), accessCode))
                .build();
    }

}