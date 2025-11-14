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

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LoginProtocol extends Provider {

    enum Error {

        /**
         * Login cancelled by the user
         */
        CANCELLED_BY_USER,
        /**
         * Applications-initiated action was canceled by the user
         */
        CANCELLED_AIA,
        /**
         * Applications-initiated action was canceled by the user. Do not send error.
         */
        CANCELLED_AIA_SILENT,
        /**
         * User is already logged-in and he has userSession in this browser. But authenticationSession is not valid anymore and hence could not continue authentication
         * in proper way. Will need to redirect back to client, so client can retry authentication. Once client retries authentication, it will usually success automatically
         * due SSO reauthentication.
         */
        ALREADY_LOGGED_IN,
        /**
         * Consent denied by the user
         */
        CONSENT_DENIED,
        /**
         * Passive authentication mode requested but nobody is logged in
         */
        PASSIVE_LOGIN_REQUIRED,
        /**
         * Passive authentication mode requested, user is logged in, but some other user interaction is necessary (eg. some required login actions exist or Consent approval is necessary for logged in
         * user)
         */
        PASSIVE_INTERACTION_REQUIRED;
    }

    LoginProtocol setSession(KeycloakSession session);

    LoginProtocol setRealm(RealmModel realm);

    LoginProtocol setUriInfo(UriInfo uriInfo);

    LoginProtocol setHttpHeaders(HttpHeaders headers);

    LoginProtocol setEventBuilder(EventBuilder event);

    Response authenticated(AuthenticationSessionModel authSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx);

    Response sendError(AuthenticationSessionModel authSession, Error error, String errorMessage);

    /**
     * Returns client data, which will be wrapped in the "clientData" parameter sent within "authentication flow" requests. The purpose of clientData is to be able to send HTTP error
     * response back to the client if authentication fails due some error and authenticationSession is not available anymore (was either expired or removed). So clientData need to contain
     * all the data to be able to send such response. For instance redirect-uri, state in case of OIDC or RelayState in case of SAML etc.
     *
     * @param authSession session from which particular clientData can be retrieved
     * @return client data, which will be wrapped in the "clientData" parameter sent within "authentication flow" requests
     */
    ClientData getClientData(AuthenticationSessionModel authSession);

    /**
     * Send the specified error to the specified client with the use of this protocol. ClientData can contain additional metadata about how to send error response to the
     * client in a correct way for particular protocol. For instance redirect-uri where to send error, state to be used in OIDC authorization endpoint response etc.
     *
     * This method is usually used when we don't have authenticationSession anymore (it was removed or expired) as otherwise it is recommended to use {@link #sendError(AuthenticationSessionModel, Error)}
     *
     * NOTE: This method should also validate if provided clientData are valid according to given client (for instance if redirect-uri is valid) as clientData is request parameter, which
     * can be injected to HTTP URLs by anyone.
     *
     * @param client client where to send error
     * @param clientData clientData with additional protocol specific metadata needed for being able to properly send error with the use of this protocol
     * @param error error to be used
     * @return response if error was sent. Null if error was not sent.
     */
    Response sendError(ClientModel client, ClientData clientData, Error error);

    Response backchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession);
    Response frontchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession);

    /**
     * This method is called when browser logout is going to be finished. It is not triggered during backchannel logout
     *
     * @param userSession user session, which was logged out
     * @param logoutSession authentication session, which was used during logout to track the logout state
     * @return response to be sent to the client
     */
    Response finishBrowserLogout(UserSessionModel userSession, AuthenticationSessionModel logoutSession);

    /**
     * @param userSession
     * @param authSession
     * @return true if SSO cookie authentication can't be used. User will need to "actively" reauthenticate
     */
    boolean requireReauthentication(UserSessionModel userSession, AuthenticationSessionModel authSession);

    /**
     * Send not-before revocation policy to the given client.
     * @param realm
     * @param resource
     * @param notBefore
     * @param managementUrl
     * @return {@code true} if revocation policy was successfully updated at the client, {@code false} otherwise.
     */
    default boolean sendPushRevocationPolicyRequest(RealmModel realm, ClientModel resource, int notBefore, String managementUrl) {
        return false;
    }

}
