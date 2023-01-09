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

package org.keycloak.adapters.springsecurity.token;

import javax.servlet.http.HttpServletResponse;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.http.HttpServletRequest;

/**
 * Creates a per-request adapter token store.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 */
public interface AdapterTokenStoreFactory {

    /**
     * Returns a new {@link AdapterTokenStore} for the given {@link KeycloakDeployment} and {@link HttpServletRequest request}.
     *
     * @param deployment the <code>KeycloakDeployment</code> (required)
     * @param request the current <code>HttpServletRequest</code> (required)
     * @param response the current <code>HttpServletResponse</code> (required when using cookies)
     *
     * @return a new <code>AdapterTokenStore</code> for the given <code>deployment</code>, <code>request</code> and <code>response</code>
     * @throws IllegalArgumentException if any required parameter is <code>null</code>
     */
    AdapterTokenStore createAdapterTokenStore(KeycloakDeployment deployment, HttpServletRequest request, HttpServletResponse response);

}
