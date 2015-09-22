/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.wsfed;

import org.keycloak.wsfed.common.WSFedConstants;
import org.keycloak.protocol.wsfed.builders.WSFedProtocolParameters;
import org.keycloak.wsfed.common.builders.WSFedResponseBuilder;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.common.util.StreamUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.InputStream;
import java.util.List;

/**
 * Created on 5/19/15.
 */
public class WSFedService {
    protected static final Logger logger = Logger.getLogger(WSFedService.class);

    protected RealmModel realm;
    private EventBuilder event;
    protected AuthenticationManager authManager;

    @Context
    protected Providers providers;
    @Context
    protected SecurityContext securityContext;
    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected HttpRequest request;
    @Context
    protected HttpResponse response;
    @Context
    protected KeycloakSession session;
    @Context
    protected ClientConnection clientConnection;

    public WSFedService(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        this.realm = realm;
        this.event = event;
        this.authManager = authManager;
    }

    /**
     */
    @GET
    public Response redirectBinding() {
        logger.debug("WS-Fed GET");
        return handleWsFedRequest();
    }


    /**
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding() {
        logger.debug("WS-Fed POST");
        return handleWsFedRequest();
    }

    @GET
    @Path("descriptor")
    @Produces(MediaType.APPLICATION_XML)
    public String getDescriptor() throws Exception {
        InputStream is = getClass().getResourceAsStream("/wsfed-idp-metadata-template.xml");
        String template = StreamUtil.readString(is);
        template = template.replace("${idp.entityID}", RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());
        template = template.replace("${idp.sso.sts}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), WSFedLoginProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.sso.passive}", RealmsResource.protocolUrl(uriInfo).build(realm.getName(), WSFedLoginProtocol.LOGIN_PROTOCOL).toString());
        template = template.replace("${idp.signing.certificate}", realm.getCertificatePem());
        return template;

    }

    protected Response basicChecks(WSFedProtocolParameters params) {
        AuthenticationManager.AuthResult authResult = authenticateIdentityCookie();

        if (!checkSsl()) {
            event.event(EventType.LOGIN);
            event.error(Errors.SSL_REQUIRED);
            return ErrorPage.error(session, Messages.HTTPS_REQUIRED);
        }
        if (!realm.isEnabled()) {
            event.event(EventType.LOGIN_ERROR);
            event.error(Errors.REALM_DISABLED);
            return ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
        }

        if (params.getWsfed_action() == null) {
            if (authResult != null && authResult.getSession().getState() == UserSessionModel.State.LOGGING_OUT) {
                params.setWsfed_action(UserSessionModel.State.LOGGING_OUT.toString());
            }
        }

        if (params.getWsfed_action() == null) {
            event.event(EventType.LOGIN);
            event.error(Errors.INVALID_REQUEST);
            return ErrorPage.error(session, Messages.INVALID_REQUEST);
        }

        if (params.getWsfed_realm() == null) {
            if(isSignout(params)) {
                //The spec says that signout doesn't require wtrealm but we generally need a way to identify the client to do SLO properly. So if wtrealm isn't passed get the user session and see if we
                //have one.
                if (authResult != null) {
                    UserSessionModel userSession = authResult.getSession();
                    params.setWsfed_realm(userSession.getNote(WSFedConstants.WSFED_REALM));
                }
            }
            else { //If it's not a signout event than wtrealm is required
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_CLIENT);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
        }

        return null;
    }

    protected boolean isSignout(WSFedProtocolParameters params) {
        return params.getWsfed_action().compareTo(WSFedConstants.WSFED_SIGNOUT_ACTION) == 0 ||
                params.getWsfed_action().compareTo(WSFedConstants.WSFED_SIGNOUT_CLEANUP_ACTION) == 0 ||
                params.getWsfed_action().compareTo(UserSessionModel.State.LOGGING_OUT.toString()) == 0;
    }

    protected Response clientChecks(ClientModel client, WSFedProtocolParameters params) {
        if(isSignout(params)) {
            return null; //client checks not required for logout
        }

        if (client == null) {
            event.event(EventType.LOGIN);
            event.error(Errors.CLIENT_NOT_FOUND);
            return ErrorPage.error(session, Messages.UNKNOWN_LOGIN_REQUESTER);
        }

        if (!client.isEnabled()) {
            event.event(EventType.LOGIN);
            event.error(Errors.CLIENT_DISABLED);
            return ErrorPage.error(session, Messages.LOGIN_REQUESTER_NOT_ENABLED);
        }
        if ((client instanceof ClientModel) && client.isBearerOnly()) {
            event.event(EventType.LOGIN);
            event.error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, Messages.BEARER_ONLY);
        }
        if (client.isDirectGrantsOnly()) {
            event.event(EventType.LOGIN);
            event.error(Errors.NOT_ALLOWED);
            return ErrorPage.error(session, Messages.DIRECT_GRANTS_ONLY);
        }

        session.getContext().setClient(client);

        return null;
    }

    public Response handleWsFedRequest() {
        MultivaluedMap<String, String> requestParams = null;
        if(request.getHttpMethod() == HttpMethod.POST) {
            requestParams = request.getFormParameters();
        }
        else {
            requestParams = uriInfo.getQueryParameters(true);
        }

        WSFedProtocolParameters params = WSFedProtocolParameters.fromParameters(requestParams);
        Response response = basicChecks(params);
        if (response != null) return response;

        ClientModel client = realm.getClientByClientId(params.getWsfed_realm());
        response = clientChecks(client, params);
        if (response != null) return response;

        if(params.getWsfed_action().compareTo(WSFedConstants.WSFED_SIGNIN_ACTION) == 0) {
            return handleLoginRequest(params, client);
        }
        else if (params.getWsfed_action().compareTo(WSFedConstants.WSFED_ATTRIBUTE_ACTION) == 0) {
            return Response.status(501).build(); //Not Implemented
        }
        else if (params.getWsfed_action().compareTo(WSFedConstants.WSFED_SIGNOUT_ACTION) == 0 ||
                 params.getWsfed_action().compareTo(WSFedConstants.WSFED_SIGNOUT_CLEANUP_ACTION) == 0) {
            logger.debug("** logout request");
            event.event(EventType.LOGOUT);

            return handleLogoutRequest(params, client);
        }
        else if (params.getWsfed_action().compareTo(UserSessionModel.State.LOGGING_OUT.toString()) == 0) {
            logger.debug("** loging out request");
            event.event(EventType.LOGOUT);

            return handleLogoutResponse(params, client);
        }
        else {
            event.event(EventType.LOGIN);
            event.error(Errors.INVALID_TOKEN);
            return ErrorPage.error(session, Messages.INVALID_REQUEST);
        }
    }

    protected Response handleLoginRequest(WSFedProtocolParameters params, ClientModel client) {
        logger.debug("** login request");
        event.event(EventType.LOGIN);

        //Essentially ACS
        String redirect = RedirectUtils.verifyRedirectUri(uriInfo, params.getWsfed_reply(), realm, client);
        if (redirect == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            return ErrorPage.error(session, Messages.INVALID_REDIRECT_URI);
        }

        ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(WSFedLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirect);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE.name());
        clientSession.setNote(ClientSessionCode.ACTION_KEY, KeycloakModelUtils.generateCodeSecret());
        clientSession.setNote(WSFedConstants.WSFED_CONTEXT, params.getWsfed_context());
        clientSession.setNote(OIDCLoginProtocol.ISSUER, RealmsResource.realmBaseUrl(uriInfo).build(realm.getName()).toString());

        return newBrowserAuthentication(clientSession);
    }

    private Response buildRedirectToIdentityProvider(String providerId, String accessCode) {
        logger.debug("Automatically redirect to identity provider: " + providerId);
        return Response.temporaryRedirect(
                Urls.identityProviderAuthnRequest(uriInfo.getBaseUri(), providerId, realm.getName(), accessCode))
                .build();
    }

    protected Response newBrowserAuthentication(ClientSessionModel clientSession) {
        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.isAuthenticateByDefault()) {
                return buildRedirectToIdentityProvider(identityProvider.getAlias(), new ClientSessionCode(realm, clientSession).getCode() );
            }
        }
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

        try {
            RestartLoginCookie.setRestartCookie(realm, clientConnection, uriInfo, clientSession);
            return processor.authenticate();
        } catch (Exception e) {
            return processor.handleBrowserException(e);
        }
    }

    protected Response handleLogoutRequest(WSFedProtocolParameters params, ClientModel client) {
        //We either need a client or a reply address to make this work
        if (client == null && params.getWsfed_reply() == null) {
            event.event(EventType.LOGIN);
            event.error(Errors.INVALID_REQUEST);
            return ErrorPage.error(session, Messages.INVALID_REQUEST);
        }

        String logoutUrl;
        if(client != null) {
            logoutUrl = RedirectUtils.verifyRedirectUri(uriInfo, params.getWsfed_reply(), realm, client);
        }
        else {
            logoutUrl = RedirectUtils.verifyRealmRedirectUri(uriInfo, params.getWsfed_reply(), realm);
        }

        AuthenticationManager.AuthResult authResult = authenticateIdentityCookie();
        if (authResult != null) {
            UserSessionModel userSession = authResult.getSession();
            userSession.setNote(WSFedLoginProtocol.WSFED_LOGOUT_BINDING_URI, logoutUrl);
            userSession.setNote(WSFedLoginProtocol.WSFED_CONTEXT, params.getWsfed_context());
            userSession.setNote(AuthenticationManager.KEYCLOAK_LOGOUT_PROTOCOL, WSFedLoginProtocol.LOGIN_PROTOCOL);

            // remove client from logout requests
            if(client != null) {
                for (ClientSessionModel clientSession : userSession.getClientSessions()) {
                    if (clientSession.getClient().getId().equals(client.getId())) {
                        clientSession.setAction(ClientSessionModel.Action.LOGGED_OUT.name());
                    }
                }
            }

            logger.debug("browser Logout");
            return authManager.browserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
        }

        //This gets called if KC has no session for the user. Essentially they are already logged out?
        WSFedResponseBuilder builder = new WSFedResponseBuilder();
        builder.setMethod(HttpMethod.GET)
                .setContext(params.getWsfed_context())
                .setDestination(logoutUrl);

        return builder.buildResponse(null);
    }

    protected Response handleLogoutResponse(WSFedProtocolParameters params, ClientModel client) {
        AuthenticationManager.AuthResult authResult = authenticateIdentityCookie();
        if (authResult == null) {
            logger.warn("Unknown ws-fed response.");
            event.event(EventType.LOGOUT);
            event.error(Errors.INVALID_TOKEN);
            return ErrorPage.error(session, Messages.INVALID_REQUEST);
        }

        // assume this is a logout response
        UserSessionModel userSession = authResult.getSession();
        if (userSession.getState() != UserSessionModel.State.LOGGING_OUT) {
            logger.warn("Unknown ws-fed response.");
            logger.warn("UserSession is not tagged as logging out.");
            event.event(EventType.LOGOUT);
            event.error(Errors.INVALID_SAML_LOGOUT_RESPONSE);
            return ErrorPage.error(session, Messages.INVALID_REQUEST);
        }

        logger.debug("logout response");
        Response response = authManager.browserLogout(session, realm, userSession, uriInfo, clientConnection, headers);
        event.success();
        return response;
    }

    /**
     * The only purpose of this method is to allow us to unit test this class
     * @return
     */
    protected AuthenticationManager.AuthResult authenticateIdentityCookie() {
        return authManager.authenticateIdentityCookie(session, realm, false);
    }

    private boolean checkSsl() {
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }
}
