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

package org.keycloak.authentication.authenticators.browser;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.util.TurnstileHelper;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * Factory for Username/Password form with Turnstile
 */
public class UsernamePasswordFormWithTurnstileFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-userpass-form-turnstile";

    @Override
    public String getDisplayType() {
        return "Username Password Form with Turnstile";
    }

    @Override
    public String getReferenceCategory() {
        return PasswordCredentialModel.TYPE;
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
        return "Validates a username and password with Cloudflare Turnstile CAPTCHA.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(TurnstileHelper.SITE_KEY)
                .label("Turnstile Site Key")
                .helpText("The site key from Cloudflare Turnstile.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(TurnstileHelper.SECRET_KEY)
                .label("Turnstile Secret")
                .helpText("The secret key from Cloudflare Turnstile.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(true)
                .add()
                .property()
                .name(TurnstileHelper.ACTION)
                .label("Action Name")
                .helpText("A meaningful name for this Turnstile context (e.g. login).")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(TurnstileHelper.DEFAULT_ACTION_LOGIN)
                .add()
                .property()
                .name(TurnstileHelper.THEME)
                .label("Theme")
                .helpText("The theme for the Turnstile widget.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options("auto", "light", "dark")
                .defaultValue(TurnstileHelper.DEFAULT_THEME)
                .add()
                .property()
                .name(TurnstileHelper.SIZE)
                .label("Size")
                .helpText("The size of the Turnstile widget.")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options("normal", "flexible", "compact")
                .defaultValue(TurnstileHelper.DEFAULT_SIZE)
                .add()
                .build();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new UsernamePasswordFormWithTurnstile(session);
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
}
