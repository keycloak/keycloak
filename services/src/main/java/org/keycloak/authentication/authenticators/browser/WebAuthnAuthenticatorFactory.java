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

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WebAuthnAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "webauthn-authenticator";

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.OPTIONAL,
            AuthenticationExecutionModel.Requirement.DISABLED,
    };

    @Override
    public String getDisplayType() {
        return "WebAuthn Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "auth";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Authenticator for WebAuthn";
    }

    public static final String USER_VERIFICATION_REQUIREMENT = "webauthn.user.verification.requirement";

   
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(USER_VERIFICATION_REQUIREMENT);
        property.setLabel("User Verification Requirement");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> verificationRequrementValues = Arrays.asList(WebAuthnConstants.OPTION_NOT_SPECIFIED, WebAuthnConstants.OPTION_REQUIRED, WebAuthnConstants.OPTION_PREFERED, WebAuthnConstants.OPTION_DISCOURAGED);
        property.setOptions(verificationRequrementValues);
        property.setDefaultValue(WebAuthnConstants.OPTION_PREFERED);
        property.setHelpText("It tells an authenticator confirm actually verifying a user.");
        return Collections.singletonList(property);
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new WebAuthnAuthenticator(session);
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
