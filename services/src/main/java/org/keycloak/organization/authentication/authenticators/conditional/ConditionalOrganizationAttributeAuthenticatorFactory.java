/*
 * Copyright 2026 Andrzej Świętek
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

package org.keycloak.organization.authentication.authenticators.conditional;

import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.organization.authentication.authenticators.conditional.OrganizationAttributeCondition.Source;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class ConditionalOrganizationAttributeAuthenticatorFactory implements ConditionalAuthenticatorFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "conditional-organization-attribute";

    public static final String CONF_ATTRIBUTE_NAME = "attribute_name";
    public static final String CONF_ATTRIBUTE_EXPECTED_VALUE = "attribute_expected_value";
    public static final String CONF_ORGANIZATION_SOURCE = "organization_source";
    public static final String CONF_NOT = "not";

    public static final String SOURCE_ANY_MEMBERSHIP = Source.ANY_MEMBERSHIP.configValue();
    public static final String SOURCE_SELECTED = Source.SELECTED.configValue();

    private static final String ORGANIZATION_SOURCE_HELP = """
            The organizations to check. '%s': every enabled organization the user is a member of.
            '%s': the organization the user is authenticating in the scope of, as resolved by the Organization Identity-First Login step, if the user is a member of it.
            With '%s', when no organization is resolved or the user is not its member, no organization is checked, so a negated condition matches.
            """.formatted(SOURCE_ANY_MEMBERSHIP, SOURCE_SELECTED, SOURCE_SELECTED);

    private static final String NEGATE_HELP = "Apply a not to the check result. A flow without a user, a realm with organizations disabled, or an incomplete or invalid condition configuration does not match either way.";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
            .name(CONF_ATTRIBUTE_NAME)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Attribute name")
            .helpText("Name of the organization attribute to check, matched exactly")
            .required(true)
            .add()
            .property()
            .name(CONF_ATTRIBUTE_EXPECTED_VALUE)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Expected attribute value")
            .helpText("Value the organization attribute must hold among its values, matched exactly")
            .required(true)
            .add()
            .property()
            .name(CONF_ORGANIZATION_SOURCE)
            .type(ProviderConfigProperty.LIST_TYPE)
            .label("Organization")
            .helpText(ORGANIZATION_SOURCE_HELP)
            .options(Source.configValues())
            .defaultValue(SOURCE_ANY_MEMBERSHIP)
            .add()
            .property()
            .name(CONF_NOT)
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .label("Negate output")
            .helpText(NEGATE_HELP)
            .add()
            .build();

    @Override
    public void init(Scope config) {
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
        return "Condition - organization attribute";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return new Requirement[]{Requirement.REQUIRED, Requirement.DISABLED};
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Flow is executed only if an organization of the user has the expected value in the configured attribute";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalOrganizationAttributeAuthenticator.SINGLETON;
    }

    @Override
    public boolean isSupported(Scope config) {
        return Profile.isFeatureEnabled(Feature.ORGANIZATION);
    }
}
