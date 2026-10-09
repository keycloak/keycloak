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

package org.keycloak.authentication;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ConfigurableAuthenticatorFactory extends ConfiguredProvider {

    AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED};

    /**
     * Config properties every authenticator execution supports, in addition to the ones returned by
     * {@link #getConfigProperties()}.
     */
    List<ProviderConfigProperty> COMMON_CONFIG_PROPERTIES = List.copyOf(ProviderConfigurationBuilder.create()
            .property()
            .name(Constants.AUTHENTICATION_EXECUTION_REFERENCE_VALUE)
            .label("authenticatorRefConfig.value.label")
            .helpText("authenticatorRefConfig.value.help")
            .type(ProviderConfigProperty.STRING_TYPE)
            .add()
            .property()
            .name(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE)
            .label("authenticatorRefConfig.maxAge.label")
            .helpText("authenticatorRefConfig.maxAge.help")
            .type(ProviderConfigProperty.STRING_TYPE)
            .add()
            .build());

    /**
     * Friendly name for the authenticator
     *
     * @return
     */
    String getDisplayType();

    /**
     * General authenticator type, i.e. totp, password, cert.
     *
     * @return null if not a referenceable category
     */
    String getReferenceCategory();

    /**
     * Optional categories that this authenticator can have (for example passkeys in username/form).
     * Optional categories are not taken into account by LoA.
     * @param session The current session in the request
     * @return Set of extra optional categories, empty by default
     */
    default Set<String> getOptionalReferenceCategories(KeycloakSession session) {
        return Collections.emptySet();
    }

    /**
     * Is this authenticator configurable?
     *
     * @return
     */
    boolean isConfigurable();

    /**
     * What requirement settings are allowed.
     *
     * @return
     */
    AuthenticationExecutionModel.Requirement[] getRequirementChoices();

    /**
     *
     * Does this authenticator have required actions that can set if the user does not have
     * this authenticator set up?
     *
     *
     * @return
     */
    boolean isUserSetupAllowed();

}
