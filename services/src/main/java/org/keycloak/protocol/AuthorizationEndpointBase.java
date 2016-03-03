/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol.Error;
import org.keycloak.services.ServicesLogger;
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

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

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

    public AuthorizationEndpointBase(RealmModel realm, EventBuilder event) {
        this.realm = realm;
        this.event = event;
    }

    protected AuthenticationProcessor createProcessor(ClientSessionModel clientSession, String flowId, String flowPath) {
        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setClientSession(clientSession)
                .setFlowPath(flowPath)
                .setFlowId(flowId)
                .setBrowserFlow(true)
                .setConnection(clientConnection)
                .setEventBuilder(event)
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
     * @param redirectToAuthentication if true redirect to flow url.  If initial call to protocol is a POST, you probably want to do this.  This is so we can disable the back button on browser
     * @return response to be returned to the browser
     */
    protected Response handleBrowserAuthenticationRequest(ClientSessionModel clientSession, LoginProtocol protocol, boolean isPassive, boolean redirectToAuthentication) {

        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.isEnabled() && identityProvider.isAuthenticateByDefault()) {
                // TODO if we are isPassive we should propagate this flag to default identity provider also if possible
                return buildRedirectToIdentityProvider(identityProvider.getAlias(), new ClientSessionCode(realm, clientSession).getCode());
            }
        }

        AuthenticationFlowModel flow = getAuthenticationFlow();
        String flowId = flow.getId();
        AuthenticationProcessor processor = createProcessor(clientSession, flowId, LoginActionsService.AUTHENTICATE_PATH);
        event.detail(Details.CODE_ID, clientSession.getId());
        if (isPassive) {
            // OIDC prompt == NONE or SAML 2 IsPassive flag
            // This means that client is just checking if the user is already completely logged in.
            // We cancel login if any authentication action or required action is required
            try {
                if (processor.authenticateOnly() == null) {
                    processor.attachSession();
                } else {
                    Response response = protocol.sendError(clientSession, Error.PASSIVE_LOGIN_REQUIRED);
                    session.sessions().removeClientSession(realm, clientSession);
                    return response;
                }
                if (processor.isActionRequired()) {
                    Response response = protocol.sendError(clientSession, Error.PASSIVE_INTERACTION_REQUIRED);
                    session.sessions().removeClientSession(realm, clientSession);
                    return response;

                }
            } catch (Exception e) {
                return processor.handleBrowserException(e);
            }
            return processor.finishAuthentication(protocol);
        } else {
            try {
                RestartLoginCookie.setRestartCookie(realm, clientConnection, uriInfo, clientSession);
                if (redirectToAuthentication) {
                    return processor.redirectToFlow();
                }
                return processor.authenticate();
            } catch (Exception e) {
                return processor.handleBrowserException(e);
            }
        }
    }

    protected AuthenticationFlowModel getAuthenticationFlow() {
        return realm.getBrowserFlow();
    }

    protected Response buildRedirectToIdentityProvider(String providerId, String accessCode) {
        logger.debug("Automatically redirect to identity provider: " + providerId);
        return Response.temporaryRedirect(
                Urls.identityProviderAuthnRequest(this.uriInfo.getBaseUri(), providerId, this.realm.getName(), accessCode))
                .build();
    }

}