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

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Collections;
import java.util.List;

public class DisplayInfoScreenAuthenticatorFactory implements AuthenticatorFactory {

    public static final String CONFIG_KEY_HEADER_LOCALIZATION_KEY = "header-localization-key";
    public static final String CONFIG_KEY_BODY_LOCALIZATION_KEY = "body-localization-key";

    public static final String PROVIDER_ID = "display-info-screen";

    private static final DisplayInfoScreenAuthenticator SINGLETON = new DisplayInfoScreenAuthenticator();

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };
    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        List<ProviderConfigProperty> list = ProviderConfigurationBuilder
                .create()

                .property().name(CONFIG_KEY_HEADER_LOCALIZATION_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Header localization key")
                .helpText("The localization key, which should be used to render the heading.")
                .required(true)
                .add()

                .property().name(CONFIG_KEY_BODY_LOCALIZATION_KEY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Body localization key")
                .helpText("The localization key, which should be used to render the body.")
                .required(true)
                .add()

                .build();
        CONFIG_PROPERTIES = Collections.unmodifiableList(list);
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
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

    @Override
    public String getReferenceCategory() {
        return PasswordCredentialModel.TYPE;
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
    public String getDisplayType() {
        return "Display info screen";
    }

    @Override
    public String getHelpText() {
        return "Displays a info screen.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

}
