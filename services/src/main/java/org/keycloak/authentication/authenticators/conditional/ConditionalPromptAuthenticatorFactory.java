/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 *  An {@link AuthenticatorFactory} for {@link ConditionalPromptAuthenticator}s.
 * 
 *  @author MilanSMA
 */
public class ConditionalPromptAuthenticatorFactory  implements ConditionalAuthenticatorFactory {

    public static final String PROVIDER_ID = "conditional-prompt";
    public static final String PROMPT_PROPERTY_NAME = "prompt-value";
    public static final String CONF_NEGATE = "negate";

    public static final String PROMPT_VALUE_NOT_DEFINED = "No prompt param";
    public static final String PROMPT_VALUE_NONE = OIDCLoginProtocol.PROMPT_VALUE_NONE;
    public static final String PROMPT_VALUE_LOGIN = OIDCLoginProtocol.PROMPT_VALUE_LOGIN;
    public static final String PROMPT_VALUE_CONSENT = OIDCLoginProtocol.PROMPT_VALUE_CONSENT;
    public static final String PROMPT_VALUE_SELECT_ACCOUNT = OIDCLoginProtocol.PROMPT_VALUE_SELECT_ACCOUNT;

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
        return "Condition - prompt param";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Condition to check the 'prompt' parameter in authentication requests";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(PROMPT_PROPERTY_NAME)
                    .type(ProviderConfigProperty.LIST_TYPE)
                    .label("prompt parameter value")
                    .helpText("'prompt' authentication request param value")
                    .required(true)
                    .defaultValue(PROMPT_VALUE_NOT_DEFINED)                    
                    .options(
                        PROMPT_VALUE_NONE,
                        PROMPT_VALUE_LOGIN,
                        PROMPT_VALUE_CONSENT,
                        PROMPT_VALUE_SELECT_ACCOUNT,
                        PROMPT_VALUE_NOT_DEFINED
                        )
                    .add()
                .property()
                    .name(CONF_NEGATE)
                    .type(ProviderConfigProperty.BOOLEAN_TYPE)
                    .label("Negate output")
                    .helpText(
                        "This condition will evaluate to true if configured prompt param is not present or its value is not maching the required value."
                    )
                    .required(true)
                    .add()
                .build();
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalPromptAuthenticator.SINGLETON;
    }
}

