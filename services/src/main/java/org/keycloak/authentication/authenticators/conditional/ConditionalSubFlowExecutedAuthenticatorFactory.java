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

package org.keycloak.authentication.authenticators.conditional;

import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * <p>Conditional factory to know if a sub-flow was executed successfully in the authentication flow.</p>
 *
 * @author rmartinc
 */
public class ConditionalSubFlowExecutedAuthenticatorFactory implements ConditionalAuthenticatorFactory {

    public static final String PROVIDER_ID = "conditional-sub-flow-executed";
    public static final String FLOW_TO_CHECK = "flow_to_check";
    public static final String CHECK_RESULT = "check_result";
    public static final String CHECK_RESULT_EXECUTED = "executed";
    public static final String CHECK_RESULT_NOT_EXECUTED = "not-executed";

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
        return "Condition - sub-flow executed";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return new Requirement[]{AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED};
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Condition to evaluate if a sub-flow was executed successfully during the authentication process";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(FLOW_TO_CHECK)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Flow name")
                .helpText("The sub-flow name to check if it was executed.")
                .required(true)
                .add()
                .property()
                .name(CHECK_RESULT)
                .type(ProviderConfigProperty.LIST_TYPE)
                .label("Check result")
                .helpText(
                        """
                        When the condition evaluates to true.
                        If 'executed' returns true when the configured sub-flow was executed with output success, false otherwise.
                        If 'not-executed' returns false when the sub-flow was executed with output success, true otherwise.
                        """
                )
                .required(true)
                .options(List.of(CHECK_RESULT_EXECUTED, CHECK_RESULT_NOT_EXECUTED))
                .defaultValue(CHECK_RESULT_EXECUTED)
                .add()
                .build();
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalSubFlowExecutedAuthenticator.SINGLETON;
    }
}
