/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstrainRefreshTokenForPublicAccessTypeExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "constrain-refresh-token-for-public-access-type";

    public static final String ONE_TIME_USE = "one-time-use";

    public static final String SENDER_CONSTRAINED = "sender-constrained";

    private static final ProviderConfigProperty ONE_TIME_USE_PROPERTY = new ProviderConfigProperty(
            ONE_TIME_USE, "One-time-use", "If On, refresh tokens for public clients is one-time use.", ProviderConfigProperty.BOOLEAN_TYPE, false);

    private static final ProviderConfigProperty SENDER_CONSTRAINED_PROPERTY = new ProviderConfigProperty(
            SENDER_CONSTRAINED, "Sender-constrained", "If On, refresh tokens for public clients is sender-constrained.", ProviderConfigProperty.BOOLEAN_TYPE, false);

    @Override
    public String getHelpText() {
        return "Refresh tokens for public clients must either be sender-constrained or one-time use";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(ONE_TIME_USE_PROPERTY, SENDER_CONSTRAINED_PROPERTY));
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new ConstrainRefreshTokenForPublicAccessTypeExecutor(session);
    }

    @Override
    public void init(Config.Scope config) {

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
}
