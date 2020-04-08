/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.ciba.channel;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.util.HttpHeaderNames;
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
import org.keycloak.protocol.ciba.utils.AuthenticationChannelResultParser;
import org.keycloak.protocol.ciba.AuthenticationChannelResult;
import org.keycloak.protocol.ciba.AuthenticationChannelStatus;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.ext.OIDCExtProvider;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.Cors;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

public abstract class HttpAuthenticationChannelProviderBase implements AuthenticationChannelProvider, OIDCExtProvider {

    private static final Logger logger = Logger.getLogger(HttpAuthenticationChannelProviderBase.class);

    protected KeycloakSession session;
    protected EventBuilder event;

    protected MultivaluedMap<String, String> formParams;

    protected RealmModel realm;

    protected ClientModel client;
    protected Map<String, String> clientAuthAttributes;

    protected Cors cors;

    public HttpAuthenticationChannelProviderBase(KeycloakSession session) {
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
    public Response processAuthenticationChannelResult() {
        event.event(EventType.LOGIN);

        HttpRequest httpRequest = session.getContext().getContextObject(HttpRequest.class);
        ClientConnection clientConnection = session.getContext().getContextObject(ClientConnection.class);

        cors = Cors.add(httpRequest).auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
        formParams = httpRequest.getDecodedFormParameters();

        checkSsl(clientConnection);
        checkRealm();
        checkClient();

        Response response = verifyAuthenticationChannelResult();
        if (response != null) return response;

        setupSessions(httpRequest, clientConnection);

        persistAuthenticationChannelResult(AuthenticationChannelStatus.SUCCEEDED);

        return cors.builder(Response.ok("", MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaderNames.CACHE_CONTROL, "no-store")
                .header(HttpHeaderNames.PRAGMA, "no-cache"))
                .build();
    }

    private void setupSessions(HttpRequest httpRequest, ClientConnection clientConnection) {
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(realm, getUserSessionIdWillBeCreated());
        // here Client Model of CD(Consumption Device) needs to be used to bind its Client Session with User Session.
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAction(AuthenticatedClientSessionModel.Action.AUTHENTICATE.name());
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, getScope());

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
        if (user.getRequiredActionsStream().count() > 0) {
            event.error(Errors.RESOLVE_REQUIRED_ACTIONS);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "Account is not fully set up", Response.Status.BAD_REQUEST);
        }

        AuthenticationManager.setClientScopesInSession(authSession);

        processor.attachSession();
        UserSessionModel userSession = processor.getUserSession();
        if (userSession == null) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "User session is not found", Response.Status.BAD_REQUEST);
        }
        userSession.getNotes().putAll(clientAuthAttributes);
        logger.tracef("CIBA Grant :: specified scopes in backchannel authentication endpoint = %s, Created User Session's id = %s, Submitted in advance User Session ID Will Be Created = %s, username = %s", getScope(), userSession.getId(), getUserSessionIdWillBeCreated(), userSession.getUser().getUsername());

        // authorization (consent)
        UserConsentModel grantedConsent = session.users().getConsentByClient(realm, user.getId(), client.getId());
        if (grantedConsent == null) {
            grantedConsent = new UserConsentModel(client);
            session.users().addConsent(realm, user.getId(), grantedConsent);
            if (logger.isTraceEnabled()) {
                grantedConsent.getGrantedClientScopes().forEach(i->logger.tracef("CIBA Grant :: Consent granted. %s", i.getName()));
            }
        }

        boolean updateConsentRequired = false;

        for (String clientScopeId : authSession.getClientScopes()) {
            ClientScopeModel clientScope = KeycloakModelUtils.findClientScopeById(realm, client, clientScopeId);
            if (clientScope != null && !grantedConsent.isClientScopeGranted(clientScope) && clientScope.isDisplayOnConsentScreen()) {
                grantedConsent.addGrantedClientScope(clientScope);
                updateConsentRequired = true;
            }
        }

        if (updateConsentRequired) {
            session.users().updateConsent(realm, user.getId(), grantedConsent);
            if (logger.isTraceEnabled()) {
                grantedConsent.getGrantedClientScopes().forEach(i->logger.tracef("CIBA Grant :: Consent updated. %s", i.getName()));
            }
        }

        event.detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED);

        event.success();
    }


    protected void persistAuthenticationChannelResult(String status) {
        AuthenticationChannelResult authenticationChannelResult = new AuthenticationChannelResult(getExpiration(), status);
        AuthenticationChannelResultParser.persistAuthenticationChannelResult(session, getAuthResultId(), authenticationChannelResult, getExpiration());
    }

    abstract protected String getScope();
    abstract protected String getUserSessionIdWillBeCreated();
    abstract protected String getUserIdToBeAuthenticated();
    abstract protected String getAuthResultId();
    abstract protected int getExpiration();

    abstract protected Response verifyAuthenticationChannelResult();

    private void checkSsl(ClientConnection clientConnection) {
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
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, cors);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();

        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }
    }
}
