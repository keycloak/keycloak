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

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.provider.Provider;

/**
 * @author Pedro Igor
 */
public interface IdentityProvider<C extends IdentityProviderModel> extends Provider {

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
    AuthenticationResponse handleRequest(AuthenticationRequest request);

    /**
     * <p>Obtains state information sent to the identity provider during the authentication request. Implementations must always
     * return the same state in order to check the validity of a response from the identity provider.</p>
     *
     * <p>This method is invoked on each response from the identity provider.</p>
     *
     * @param request The request sent by the identity provider in a response to an authentication request.
     * @return
     */
    String getRelayState(AuthenticationRequest request);

    /**
     * <p>Handles a response from the identity provider after a successful authentication request is made. Usually, the response will
     * contain all the necessary information in order to trust the authentication performed by the identity provider and resolve
     * the identity information for the authenticating user.</p>
     *
     * <p>If the response is trusted and proves user's authenticity, this method may return a
     * {@link FederatedIdentity} in the response. In this case, the authentication flow stops.</p>
     *
     * @param request
     * @return
     */
    AuthenticationResponse handleResponse(AuthenticationRequest request);
}
