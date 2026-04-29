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

package org.keycloak.authentication;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.Provider;

/**
 * This interface is for users that want to add custom client authenticators to an authentication flow.
 * You must implement this interface as well as a ClientAuthenticatorFactory.
 *
 * This interface is for verifying client credentials from request. On the adapter side, you must also implement org.keycloak.protocol.oidc.client.authentication.ClientCredentialsProvider , which is supposed
 * to add the client credentials to the request, which will ClientAuthenticator verify on server side
 *
 * @see org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator
 * @see org.keycloak.authentication.authenticators.client.JWTClientAuthenticator
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientAuthenticator extends Provider {

    /**
     * Attempts to identify and look up the client from the current HTTP request.
     *
     * <p>This method should extract the client identity from the request (e.g., from parameters,
     * headers, or tokens) and look up the corresponding {@link ClientModel}. It must not validate
     * client credentials or mutate the authentication flow status (i.e., do not call
     * {@code context.success()}, {@code context.failure()}, or {@code context.challenge()}).</p>
     *
     * <p>The default implementation returns {@code null}, which signals the authentication flow
     * to fall back to the legacy single-pass behavior where {@link #authenticateClient} handles
     * both client lookup and credential validation. Custom authenticator implementations should
     * override this method to benefit from the two-phase authentication flow.</p>
     *
     * @param context the client authentication flow context providing access to the HTTP request, realm, and session
     * @return the identified {@link ClientModel}, or {@code null} if this authenticator cannot identify a client from the request
     */
    default ClientModel lookupClient(ClientAuthenticationFlowContext context) {
        return null;
    }

    /**
     * Validates the client credentials from the current HTTP request.
     *
     * <p>When used with the two-phase authentication flow, the client will already be identified
     * and set on the context via {@link ClientAuthenticationFlowContext#getClient()} before this
     * method is called. Implementations should validate the client's credentials and call
     * {@code context.success()} or {@code context.failure()} accordingly.</p>
     *
     * <p>For backward compatibility, if {@link #lookupClient} is not overridden, this method
     * may still be called with no client set on the context, in which case it should handle
     * both client lookup and credential validation (legacy behavior).</p>
     *
     * @param context the client authentication flow context
     */
    void authenticateClient(ClientAuthenticationFlowContext context);

}
