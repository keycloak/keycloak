/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.provider;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public interface IdentityProvider<C extends IdentityProviderModel> extends Provider {

    public interface AuthenticationCallback {
        /**
         * This method should be called by provider after the JAXRS callback endpoint has finished authentication
         * with the remote IDP
         *
         * @param userNotes notes to add to the UserSessionModel
         * @param identityProviderConfig provider config
         * @param federatedIdentity federated identity
         * @param code relayState or state parameter used to identity the client session
         * @return
         */
        public Response authenticated(Map<String, String> userNotes, IdentityProviderModel identityProviderConfig, FederatedIdentity federatedIdentity, String code);
    }

    /**
     * JAXRS callback endpoint for when the remote IDP wants to callback to keycloak.
     *
     * @return
     */
    Object callback(RealmModel realm, AuthenticationCallback callback);

    /**
     * <p>Initiates the authentication process by sending an authentication request to an identity provider. This method is called
     * only once during the authentication.</p>
     *
     * <p>Depending on how the authentication is performed, this method may redirect the user to the identity provider for authentication.
     * In this case, the response would contain a {@link javax.ws.rs.core.Response} that will be used to redirect the user.</p>
     *
     * <p>However, if the authentication flow does not require a redirect to the identity provider (eg.: simple challenge/response mechanism), this method may return a response containing
     * a {@link FederatedIdentity} representing the identity information for an user. In this case, the authentication flow stops.</p>
     *
     * @param request The initial authentication request. Contains all the contextual information in order to build an authentication request to the
 *                    identity provider.
     * @return
     */
    Response handleRequest(AuthenticationRequest request);

    /**
     * <p>Returns a {@link javax.ws.rs.core.Response} containing the token previously stored during the authentication process for a
     * specific user.</p>
     *
     * @param identity
     * @return
     */
    Response retrieveToken(FederatedIdentityModel identity);

    /**
     * Called when a Keycloak application initiates a logout through the browser.  This is expected to do a logout
     * with the IDP
     *
     * @param userSession
     * @param uriInfo
     * @param realm
     * @return null if this is not supported by this provider
     */
    Response keycloakInitiatedBrowserLogout(UserSessionModel userSession, UriInfo uriInfo, RealmModel realm);

    /**
     * Export a representation of the IdentityProvider in a specific format.  For example, a SAML EntityDescriptor
     *
     * @return
     */
    Response export(UriInfo uriInfo, RealmModel realm, String format);

}
