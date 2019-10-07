/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.requiredactions;

import org.keycloak.OAuth2Constants;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class WebAuthnRegisterFactory implements RequiredActionFactory, DisplayTypeRequiredActionFactory {
    public static final String PROVIDER_ID = "webauthn-register";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new WebAuthnRegister(session);
    }

    @Override
    public void init(Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        if (displayType == null) return create(session);
        if (!OAuth2Constants.DISPLAY_CONSOLE.equalsIgnoreCase(displayType)) return null;
        // TODO : write console typed provider?
        return null;
    }

    @Override
    public String getDisplayText() {
        return "Webauthn Register";
    }

}
