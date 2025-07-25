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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for OAuth 2.0 Refresh Token Grant
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class RefreshTokenGrantTypeFactory implements OAuth2GrantTypeFactory {

    public static final String GRANT_SHORTCUT = "rt";

    @Override
    public String getId() {
        return OAuth2Constants.REFRESH_TOKEN;
    }

    @Override
    public String getShortcut() {
        return GRANT_SHORTCUT;
    }

    @Override
    public OAuth2GrantType create(KeycloakSession session) {
        return new RefreshTokenGrantType();
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
