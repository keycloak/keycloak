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

package org.keycloak.authentication.authenticators.browser.risk.authn;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class RiskScoreAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "risk-score-authenticator";
    private static final RiskScoreAuthenticator SINGLETON = new RiskScoreAuthenticator();

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
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
    public String getDisplayType() {
        return "Risk Score Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return null;
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
        return "Evaluates contextual login risk and adapts authentication by allowing, requiring MFA, or blocking the login attempt.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                integer(AdaptiveAuthPolicy.LOW_RISK_THRESHOLD, "Low risk threshold",
                        "Score at or above this value triggers medium-risk step-up behavior.",
                        AdaptiveAuthPolicy.DEFAULT_LOW_RISK_THRESHOLD),
                integer(AdaptiveAuthPolicy.HIGH_RISK_THRESHOLD, "High risk threshold",
                        "Score at or above this value blocks authentication.",
                        AdaptiveAuthPolicy.DEFAULT_HIGH_RISK_THRESHOLD),
                integer(AdaptiveAuthPolicy.FAILED_ATTEMPTS_WEIGHT, "Failed attempts weight",
                        "Weight applied to the failed login attempts strategy.",
                        AdaptiveAuthPolicy.DEFAULT_FAILED_ATTEMPTS_WEIGHT),
                integer(AdaptiveAuthPolicy.IP_RISK_WEIGHT, "IP risk weight",
                        "Weight applied to the source IP strategy.",
                        AdaptiveAuthPolicy.DEFAULT_IP_RISK_WEIGHT),
                integer(AdaptiveAuthPolicy.DEVICE_RISK_WEIGHT, "Device risk weight",
                        "Weight applied to the device/browser strategy.",
                        AdaptiveAuthPolicy.DEFAULT_DEVICE_RISK_WEIGHT),
                integer(AdaptiveAuthPolicy.BEHAVIOR_RISK_WEIGHT, "Behavior risk weight",
                        "Weight applied to the login-time behavior strategy.",
                        AdaptiveAuthPolicy.DEFAULT_BEHAVIOR_RISK_WEIGHT),
                integer(AdaptiveAuthPolicy.GEO_RISK_WEIGHT, "Geo risk weight",
                        "Weight applied to the header-based geolocation strategy.",
                        AdaptiveAuthPolicy.DEFAULT_GEO_RISK_WEIGHT),
                integer(AdaptiveAuthPolicy.FAILED_ATTEMPTS_THRESHOLD, "Failed attempts threshold",
                        "Failure count that maps failed-attempt risk to a high raw score.",
                        AdaptiveAuthPolicy.DEFAULT_FAILED_ATTEMPTS_THRESHOLD),
                integer(AdaptiveAuthPolicy.NEW_IP_RISK_SCORE, "New IP risk score",
                        "Raw risk score used when the source IP differs from recent successful login history.",
                        AdaptiveAuthPolicy.DEFAULT_NEW_IP_RISK_SCORE),
                integer(AdaptiveAuthPolicy.NEW_DEVICE_RISK_SCORE, "New device risk score",
                        "Raw risk score used when the browser/device fingerprint differs from recent successful login history.",
                        AdaptiveAuthPolicy.DEFAULT_NEW_DEVICE_RISK_SCORE),
                integer(AdaptiveAuthPolicy.UNUSUAL_LOGIN_START_HOUR, "Unusual login start hour",
                        "Start hour, in UTC, for the unusual login window.",
                        AdaptiveAuthPolicy.DEFAULT_UNUSUAL_LOGIN_START_HOUR),
                integer(AdaptiveAuthPolicy.UNUSUAL_LOGIN_END_HOUR, "Unusual login end hour",
                        "End hour, in UTC, for the unusual login window.",
                        AdaptiveAuthPolicy.DEFAULT_UNUSUAL_LOGIN_END_HOUR),
                integer(AdaptiveAuthPolicy.UNUSUAL_LOGIN_RISK_SCORE, "Unusual login risk score",
                        "Raw risk score used when login time is inside the unusual login window.",
                        AdaptiveAuthPolicy.DEFAULT_UNUSUAL_LOGIN_RISK_SCORE),
                integer(AdaptiveAuthPolicy.NEW_GEO_RISK_SCORE, "New geo risk score",
                        "Raw risk score used when the optional geography header differs from recent successful login history.",
                        AdaptiveAuthPolicy.DEFAULT_NEW_GEO_RISK_SCORE),
                integer(AdaptiveAuthPolicy.HISTORY_LOOKBACK_LIMIT, "History lookback limit",
                        "Maximum number of recent login events to inspect for history-based strategies.",
                        AdaptiveAuthPolicy.DEFAULT_HISTORY_LOOKBACK_LIMIT));
    }

    private static ProviderConfigProperty integer(String name, String label, String helpText, int defaultValue) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name);
        property.setLabel(label);
        property.setHelpText(helpText);
        property.setType(ProviderConfigProperty.INTEGER_TYPE);
        property.setDefaultValue(Integer.toString(defaultValue));
        return property;
    }
}
