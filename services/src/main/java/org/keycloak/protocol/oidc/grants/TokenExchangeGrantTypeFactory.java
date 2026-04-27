/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;


import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * Factory for OAuth 2.0 Authorization Code Grant
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class TokenExchangeGrantTypeFactory implements OAuth2GrantTypeFactory, EnvironmentDependentProviderFactory {

    @Override
    public String getId() {
        return OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE;
    }

    @Override
    public String getShortcut() {
        return "te";
    }

    @Override
    public OAuth2GrantType create(KeycloakSession session) {
        return new TokenExchangeGrantType();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return     Profile.isFeatureEnabled(Profile.Feature.TOKEN_EXCHANGE)
                || Profile.isFeatureEnabled(Profile.Feature.TOKEN_EXCHANGE_STANDARD_V2);
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

}
