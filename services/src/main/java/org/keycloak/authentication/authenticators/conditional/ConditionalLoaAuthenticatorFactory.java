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

package org.keycloak.authentication.authenticators.conditional;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowCallbackFactory;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class ConditionalLoaAuthenticatorFactory implements ConditionalAuthenticatorFactory, AuthenticationFlowCallbackFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "conditional-level-of-authentication";
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    private static final List<ProviderConfigProperty> CONFIG = ProviderConfigurationBuilder.create()
            .property()
            .name(ConditionalLoaAuthenticator.LEVEL)
            .label(ConditionalLoaAuthenticator.LEVEL)
            .helpText(ConditionalLoaAuthenticator.LEVEL + ".tooltip")
            .type(ProviderConfigProperty.STRING_TYPE)
            .add()
            .property()
            .name(ConditionalLoaAuthenticator.MAX_AGE)
            .label(ConditionalLoaAuthenticator.MAX_AGE)
            .helpText(ConditionalLoaAuthenticator.MAX_AGE + ".tooltip")
            .type(ProviderConfigProperty.STRING_TYPE)
            .defaultValue(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE) // 10 hours
            .add()
            .build();

    @Override
    public Authenticator create(KeycloakSession session) {
        return new ConditionalLoaAuthenticator(session);
    }

    @Override
    public void init(Config.Scope config) { }

    @Override
    public void postInit(KeycloakSessionFactory factory) { }

    @Override
    public void close() { }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Condition - Level of Authentication";
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
        return false;
    }

    @Override
    public String getHelpText() {
        return "Flow is executed only if the configured LOA or a higher one has been requested but not yet satisfied. After the flow is successfully finished, the LOA in the session will be updated to value prescribed by this condition.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG;
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        // NOP - instance created in create() method
        return null;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.STEP_UP_AUTHENTICATION);
    }
}
