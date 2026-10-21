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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * <p>Conditional authenticator that checks if a list of configured credentials has been
 * used (or not used) by the user in the authentication process. This way the sub-flow
 * can be executed only when the user has used a specific credential type
 * (password for example) or not used a specific credential type (otp for
 * example).</p>
 *
 * @author rmartinc
 */
public class ConditionalCredentialAuthenticatorFactory implements ConditionalAuthenticatorFactory {

    public static final String PROVIDER_ID = "conditional-credential";

    public static final String CONF_CREDENTIALS = "credentials";
    public static final String CONF_INCLUDED = "included";
    public static final String NONE_CREDENTIAL = "none";

    private List<String> credentialList;

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory sessionFactory) {
        credentialList = getCredentialList(sessionFactory);
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
        return "Condition - credential";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Condition to evaluate if a specific credential type has been used (or not used) by the user during the authentication process";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CONF_CREDENTIALS)
                .type(ProviderConfigProperty.MULTIVALUED_LIST_TYPE)
                .options(credentialList)
                .label("Credentials")
                .helpText("The list of credentials to be considered by the condition.")
                .required(Boolean.TRUE)
                .add()

                .property()
                .name(CONF_INCLUDED)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("Included")
                .helpText(
                        """
                        If this option is true, the condition will be evaluated to true when any of the credentials specified in the credentials option
                        has been used in the authentication process, false otherwise.
                        If this option is false, the condition is evaluated in the opposite way, it will be true if none of the credentials configured
                        have been used, and false if one or more of them have been used.
                        """
                )
                .add()
                .build();
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalCredentialAuthenticator.SINGLETON;
    }

    private static List<String> getCredentialList(KeycloakSessionFactory sessionFactory) {
        // the list of credentials in the installation plus the fixed ones (kerberos, cert and none)
        try (KeycloakSession session = sessionFactory.create()) {
            return Stream.concat(
                    AuthenticatorUtil.getCredentialProviders(session).map(CredentialProvider::getType),
                    Stream.of(UserCredentialModel.KERBEROS, UserCredentialModel.CLIENT_CERT, NONE_CREDENTIAL)
            ).collect(Collectors.toList());
        }
    }
}
