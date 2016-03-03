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

package org.keycloak.adapters;

import org.keycloak.adapters.spi.AdapterSessionStore;

/**
 * Abstraction for storing token info on adapter side. Intended to be per-request object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AdapterTokenStore extends AdapterSessionStore {

    /**
     * Impl can validate if current token exists and perform refreshing if it exists and is expired
     */
    void checkCurrentToken();

    /**
     * Check if we are logged already (we have already valid and successfully refreshed accessToken). Establish security context if yes
     *
     * @param authenticator used for actual request authentication
     * @return true if we are logged-in already
     */
    boolean isCached(RequestAuthenticator authenticator);

    /**
     * Finish successful OAuth2 login and store validated account
     *
     * @param account
     */
    void saveAccountInfo(OidcKeycloakAccount account);

    /**
     * Handle logout on store side and possibly propagate logout call to Keycloak
     */
    void logout();

    /**
     * Callback invoked after successful token refresh
     *
     * @param securityContext context where refresh was performed
     */
    void refreshCallback(RefreshableKeycloakSecurityContext securityContext);

}
