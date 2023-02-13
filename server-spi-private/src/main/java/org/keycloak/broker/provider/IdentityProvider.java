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
package org.keycloak.broker.provider;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Pedro Igor
 */
public interface IdentityProvider<C extends IdentityProviderModel> extends Provider {

    String EXTERNAL_IDENTITY_PROVIDER = "EXTERNAL_IDENTITY_PROVIDER";
    String FEDERATED_ACCESS_TOKEN = "FEDERATED_ACCESS_TOKEN";

    interface AuthenticationCallback {

        /**
         * Common method to return current authenticationSession and verify if it is not expired
         *
         * @param encodedCode
         * @return see description
         */
        AuthenticationSessionModel getAndVerifyAuthenticationSession(String encodedCode);

        /**
         * This method should be called by provider after the JAXRS callback endpoint has finished authentication
         * with the remote IDP. There is an assumption that authenticationSession is set in the context when this method is called
         *
         * @param context
         * @return see description
         */
        Response authenticated(BrokeredIdentityContext context);

        /**
         * Called when user cancelled authentication on the IDP side - for example user didn't approve consent page on the IDP side.
         * Assumption is that authenticationSession is set in the {@link org.keycloak.models.KeycloakContext} when this method is called
         *
         * @return see description
         */
        Response cancelled();

        /**
         * Called when error happened on the IDP side.
         * Assumption is that authenticationSession is set in the {@link org.keycloak.models.KeycloakContext} when this method is called
         *
         * @return see description
         */
        Response error(String message);
    }


    void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context);
    void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context);
    void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context);
    void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, BrokeredIdentityContext context);

    /**
     * JAXRS callback endpoint for when the remote IDP wants to callback to keycloak.
     *
     * @return
     */
    Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event);

    /**
     * <p>Initiates the authentication process by sending an authentication request to an identity provider. This method is called
     * only once during the authentication.</p>
     *
     * @param request The initial authentication request. Contains all the contextual information in order to build an authentication request to the
 *                    identity provider.
     * @return
     */
    Response performLogin(AuthenticationRequest request);

    /**
     * <p>Returns a {@link javax.ws.rs.core.Response} containing the token previously stored during the authentication process for a
     * specific user.</p>
     *
     * @param identity
     * @return
     */
    Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity);

    void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm);

    /**
     * Called when a Keycloak application initiates a logout through the browser.  This is expected to do a logout
     * with the IDP
     *
     * @param userSession
     * @param uriInfo
     * @param realm
     * @return null if this is not supported by this provider
     */
    Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm);

    /**
     * Export a representation of the IdentityProvider in a specific format.  For example, a SAML EntityDescriptor
     *
     * @return
     */
    Response export(UriInfo uriInfo, RealmModel realm, String format);

    /**
     * Implementation of marshaller to serialize/deserialize attached data to Strings, which can be saved in clientSession
     * @return
     */
    IdentityProviderDataMarshaller getMarshaller();

}
