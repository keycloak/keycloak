/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class SecureLogoutExecutorFactory implements ClientPolicyExecutorProviderFactory  {

    public static final String PROVIDER_ID = "secure-logout";

    public static final String ALLOW_FRONT_CHANNEL_LOGOUT = "allow-front-channel-logout";

    private static final ProviderConfigProperty ALLOW_FRONT_CHANNEL_LOGOUT_PROPERTY = new ProviderConfigProperty(
            ALLOW_FRONT_CHANNEL_LOGOUT, "Allow Front-Channel Logout", "If On, then front-channel logout should be allowed. Otherwise, clients should favor other logout mechanisms such as back-channel logout.", ProviderConfigProperty.BOOLEAN_TYPE, false);

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureLogoutExecutor(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Enforces certain constraints on how clients should support logout.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.singletonList(ALLOW_FRONT_CHANNEL_LOGOUT_PROPERTY);
    }

}
