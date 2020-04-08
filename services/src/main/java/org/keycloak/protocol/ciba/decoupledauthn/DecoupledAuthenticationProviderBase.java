package org.keycloak.protocol.ciba.decoupledauthn;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ciba.utils.DecoupledAuthnResult;
import org.keycloak.protocol.ciba.utils.DecoupledAuthnResultParser;
import org.keycloak.protocol.ciba.utils.DecoupledAuthStatus;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.ext.OIDCExtProvider;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

public abstract class DecoupledAuthenticationProviderBase implements DecoupledAuthenticationProvider, OIDCExtProvider {

    private static final Logger logger = Logger.getLogger(DecoupledAuthenticationProviderBase.class);

    protected KeycloakSession session;
    protected EventBuilder event;

    protected MultivaluedMap<String, String> formParams;

    protected RealmModel realm;

    protected ClientModel client;
    protected Map<String, String> clientAuthAttributes;

    @Context
    protected HttpHeaders headers;
    @Context
    protected HttpRequest httpRequest;
    @Context
    protected HttpResponse httpResponse;
    @Context
    protected ClientConnection clientConnection;

    protected Cors cors;

    public DecoupledAuthenticationProviderBase(KeycloakSession session) {
        this.session = session;
        realm = session.getContext().getRealm();
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public void close() {
    }

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processDecoupledAuthnResult() {
        event.event(EventType.LOGIN);
        headers = session.getContext().getContextObject(HttpHeaders.class);
        httpRequest = session.getContext().getContextObject(HttpRequest.class);
        httpResponse = session.getContext().getContextObject(HttpResponse.class);
        clientConnection = session.getContext().getContextObject(ClientConnection.class);

        cors = Cors.add(httpRequest).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
        formParams = httpRequest.getDecodedFormParameters();
        dumpMultivaluedMap(formParams);

        checkSsl();
        checkRealm();
        // here Client Model of Decoupled Authentication Server is set to this class member "client".
        // this will be overridden by Client Model of CD(Consumer Device).
        checkClient();
        logger.info(" client_id = " + client.getClientId());

        MultivaluedMap<String, Object> outputHeaders = httpResponse.getOutputHeaders();
        outputHeaders.putSingle("Cache-Control", "no-store");
        outputHeaders.putSingle("Pragma", "no-cache");

        Response response = verifyDecoupledAuthnResult();
        if (response != null) return response;

        setupSessions();

        persistDecoupledAuthenticationResult(DecoupledAuthStatus.SUCCEEDED);

        return cors.builder(Response.ok("", MediaType.APPLICATION_JSON_TYPE)).build();
    }

    private void setupSessions() {
        //RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(getUserSessionIdWillBeCreated(), realm);
        // here Client Model of CD(Consumer Device) needs to be used to bind its Client Session with User Session.
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAction(AuthenticatedClientSessionModel.Action.AUTHENTICATE.name());
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, getScope());
        authSession.setAuthNote("convey_user", "Nein");
        logger.info(" specified scopes in backchannel authentication endpoint = " + getScope());

        // authentication
        AuthenticationFlowModel flow = AuthenticationFlowResolver.resolveCIBAFlow(authSession);
        String flowId = flow.getId();
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setAuthenticationSession(authSession)
                .setFlowId(flowId)
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(httpRequest);

        processor.authenticateOnly();
        processor.evaluateRequiredActionTriggers();
        UserModel user = authSession.getAuthenticatedUser();
        if (user.getRequiredActions() != null && user.getRequiredActions().size() > 0) {
            event.error(Errors.RESOLVE_REQUIRED_ACTIONS);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Account is not fully set up", Response.Status.BAD_REQUEST);
        }

        AuthenticationManager.setClientScopesInSession(authSession);

        processor.attachSession();
        UserSessionModel userSession = processor.getUserSession();
        if (userSession == null) {
            logger.info(" userSession is null.");
        } else {
            logger.info(" user session id = " + userSession.getId() + ", username = " + userSession.getUser().getUsername());
        }
        updateUserSessionFromClientAuth(userSession);

        logger.info("  Created User Session's id                            = " + userSession.getId());
        logger.info("  Submitted in advance User Session ID Will Be Created = " + getUserSessionIdWillBeCreated());

        // authorization (consent)
        UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());
        if (grantedConsent == null) {
            grantedConsent = new UserConsentModel(client);
            session.users().addConsent(realm, user.getId(), grantedConsent);
            logger.info("  Consent updated : ");
            grantedConsent.getGrantedClientScopes().stream().forEach(i->logger.info(i.getName()));
        }

        boolean updateConsentRequired = false;

        for (String clientScopeId : authSession.getClientScopes()) {
            ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(realm, client, clientScopeId);
            if (clientScope != null) {
                if (!grantedConsent.isClientScopeGranted(clientScope) && clientScope.isDisplayOnConsentScreen()) {
                    grantedConsent.addGrantedClientScope(clientScope);
                    updateConsentRequired = true;
                }
            } else {
                logger.warnf("Client scope or client with ID '%s' not found", clientScopeId);
            }
        }

        if (updateConsentRequired) {
            session.users().updateConsent(realm, user.getId(), grantedConsent);
            logger.info("  Consent granted : ");
            grantedConsent.getGrantedClientScopes().stream().forEach(i->logger.info(i.getName()));
        }

        event.detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        event.success();
    }


    protected void persistDecoupledAuthenticationResult(String status) {
        DecoupledAuthnResult decoupledAuthnResult = new DecoupledAuthnResult(getExpiration(), status);
        DecoupledAuthnResultParser.persistDecoupledAuthnResult(session, getAuthResultId(), decoupledAuthnResult, getExpiration());
    }

    abstract protected String getScope();
    abstract protected String getUserSessionIdWillBeCreated();
    abstract protected String getUserIdToBeAuthenticated();
    abstract protected String getAuthResultId();
    abstract protected int getExpiration();

    abstract protected Response verifyDecoupledAuthnResult();

    private void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), "access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();

        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }
    }

    private void dumpMultivaluedMap(MultivaluedMap<String, String> params) {
        Set<String> keys = params.keySet();
        keys.stream().forEach(i -> {
            logger.info("key = " + i);
            params.get(i).stream().forEach(j -> logger.info("value = " + j));
        });
    }

}
