/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class ConditionalUserAttributeValueFactory implements ConditionalAuthenticatorFactory {

    public static final String PROVIDER_ID = "conditional-user-attribute";

    public static final String CONF_ATTRIBUTE_NAME = "attribute_name";
    public static final String CONF_ATTRIBUTE_EXPECTED_VALUE = "attribute_expected_value";
    public static final String CONF_INCLUDE_GROUP_ATTRIBUTES = "include_group_attributes";
    public static final String CONF_NOT = "not";
    public static final String REGEX = "regex";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Condition - user attribute";
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
        return "Flow is executed only if the user attribute exists and has the expected value";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty authNoteName = new ProviderConfigProperty();
        authNoteName.setType(ProviderConfigProperty.STRING_TYPE);
        authNoteName.setName(CONF_ATTRIBUTE_NAME);
        authNoteName.setLabel("Attribute name");
        authNoteName.setHelpText("Name of the attribute to check");

        ProviderConfigProperty authNoteExpectedValue = new ProviderConfigProperty();
        authNoteExpectedValue.setType(ProviderConfigProperty.STRING_TYPE);
        authNoteExpectedValue.setName(CONF_ATTRIBUTE_EXPECTED_VALUE);
        authNoteExpectedValue.setLabel("Expected attribute value");
        authNoteExpectedValue.setHelpText("Expected value in the attribute");

        ProviderConfigProperty includeGroupAttributes = new ProviderConfigProperty();
        includeGroupAttributes.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        includeGroupAttributes.setName(CONF_INCLUDE_GROUP_ATTRIBUTES);
        includeGroupAttributes.setLabel("Include group attributes");
        includeGroupAttributes.setHelpText("If On, the condition checks if any of the joined groups has one attribute matching the configured name and value (this option can affect performance)");

        ProviderConfigProperty negateOutput = new ProviderConfigProperty();
        negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutput.setName(CONF_NOT);
        negateOutput.setLabel("Negate output");
        negateOutput.setHelpText("Apply a not to the check result");

        ProviderConfigProperty regexOutput = new ProviderConfigProperty();
        regexOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        regexOutput.setName(REGEX);
        regexOutput.setLabel(REGEX);
        regexOutput.setHelpText("Check equality with regex");

        return Arrays.asList(authNoteName, authNoteExpectedValue, includeGroupAttributes, negateOutput, regexOutput);
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalUserAttributeValue.SINGLETON;
    }
}
