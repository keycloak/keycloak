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

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class SetHiddenBrokerAuthenticatorFactory implements AuthenticatorFactory {
    private static final SetHiddenBrokerAuthenticator INSTANCE = new SetHiddenBrokerAuthenticator();
    public static final String PROVIDER_ID = "set-hidden-broker";
    public static final String HIDDEN_BROKER_CONFIG = "hidden";

    static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED};

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public String getDisplayType() {
        return "Set hidden Broker";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getReferenceCategory() {
        return null;
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
        return "Hide log in via social buttons on login page";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty hiddenBrokers = new ProviderConfigProperty(
                HIDDEN_BROKER_CONFIG,
                "Hidden brokers",
                "What social brokers should be hidden on login page. Use alias",
                ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
                null,
                false,
                true
        );

        return List.of(hiddenBrokers);
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
