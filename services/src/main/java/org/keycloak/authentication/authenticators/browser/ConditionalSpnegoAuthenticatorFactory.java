/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
 *
 */
package org.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

import static org.keycloak.authentication.authenticators.browser.ConditionalSpnegoAuthenticator.WHITELIST_PATTERN;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

/**
 * An {@link AuthenticatorFactory} for {@link ConditionalSpnegoAuthenticator}s.
 *
 * @author <a href="mailto:Ryan.Slominski@gmail.com">Ryan Slominski</a>
 */
public class ConditionalSpnegoAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "conditional-auth-spnego";
    static ConditionalSpnegoAuthenticator SINGLETON = new ConditionalSpnegoAuthenticator();

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
        return UserCredentialModel.KERBEROS;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED};

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Conditional SPNEGO";
    }

    @Override
    public String getHelpText() {
        return "Conditionally attempt SPNEGO based on existence of prompt=login and optionally a configured whitelist regex pattern to match against the first X-Forwarded-For";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty whitelistPattern = new ProviderConfigProperty();
        whitelistPattern.setType(STRING_TYPE);
        whitelistPattern.setName(WHITELIST_PATTERN);
        whitelistPattern.setLabel("Whitelist Regex Pattern");
        whitelistPattern.setHelpText("Whitelist Regex pattern to match against X-Forwarded-For HTTP header.  If pattern is empty SPNEGO is allowed, else only hosts matching the pattern have SPNEGO attempted.");
        whitelistPattern.setDefaultValue("");

        return Arrays.asList(whitelistPattern);
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }    
}
