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

import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.enums.TokenStore;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link AdapterTokenStoreFactory} that returns a new {@link SpringSecurityTokenStore} for each request.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 */
public class SpringSecurityAdapterTokenStoreFactory implements AdapterTokenStoreFactory {

    @Override
    public AdapterTokenStore createAdapterTokenStore(KeycloakDeployment deployment, HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(deployment, "KeycloakDeployment is required");
        if (deployment.getTokenStore() == TokenStore.COOKIE) {
            return new SpringSecurityCookieTokenStore(deployment, request, response);
        }
        return new SpringSecurityTokenStore(deployment, request);
    }
}
