/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

public class RiskScoreAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "risk-score-authenticator";
    public static final String FAILURE_THRESHOLD = "failureThreshold";
    public static final RiskScoreAuthenticator SINGLETON = new RiskScoreAuthenticator();

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

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
        return OTPCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Risk Score Authenticator";
    }

    @Override
    public String getHelpText() {
        return "Requires OTP authentication when the configured recent login failure threshold is reached.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty failureThreshold = new ProviderConfigProperty();
        failureThreshold.setName(FAILURE_THRESHOLD);
        failureThreshold.setLabel("Login failure threshold");
        failureThreshold.setHelpText("Number of recent login failures that triggers an OTP challenge.");
        failureThreshold.setType(ProviderConfigProperty.INTEGER_TYPE);
        failureThreshold.setDefaultValue(Integer.toString(RiskScoreAuthenticator.DEFAULT_FAILURE_THRESHOLD));

        return List.of(failureThreshold);
    }
}
