/*
 *  Copyright 2021 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import jakarta.ws.rs.core.Response;

import org.keycloak.provider.Provider;

/**
 * Provides token exchange mechanism for supported tokens
 *
 * @author <a href="mailto:dmitryt@backbase.com">Dmitry Telegin</a>
 */
public interface TokenExchangeProvider extends Provider {

    /**
     * Check if exchange request is supported by this provider
     *
     * @param context token exchange context
     * @return true if the request is supported
     */
    boolean supports(TokenExchangeContext context);

    /**
     * Exchange the <code>token</code>.
     *
     * @param context
     * @return response with a new token
     */
    Response exchange(TokenExchangeContext context);

    /**
     * @return version of the token-exchange provider. Could be useful by various components (like for example identity-providers), which need to interact with the token-exchange provider
     * to doublecheck if it should have a "legacy" behaviour (for older version of token-exchange provider) or a "new" behaviour
     */
    int getVersion();

}
