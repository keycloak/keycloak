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

package org.keycloak.authentication.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.authenticators.util.TurnstileHelper;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import org.jboss.logging.Logger;

/**
 * Adds Cloudflare Turnstile CAPTCHA to registration forms.
 */
public class RegistrationTurnstile implements FormAction, FormActionFactory {

    private static final Logger LOGGER = Logger.getLogger(RegistrationTurnstile.class);
    public static final String PROVIDER_ID = "registration-turnstile-action";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Turnstile";
    }

    @Override
    public String getHelpText() {
        return "Adds Cloudflare Turnstile to the form.";
    }

    @Override
    public String getReferenceCategory() {
        return TurnstileHelper.TURNSTILE_REFERENCE_CATEGORY;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        LOGGER.trace("Building page with Turnstile");

        Map<String, String> config = null;
        if (context.getAuthenticatorConfig() != null) {
            config = context.getAuthenticatorConfig().getConfig();
        }

        if (config == null) {
            form.addError(new FormMessage(null, Messages.TURNSTILE_NOT_CONFIGURED));
            return;
        }

        if (!TurnstileHelper.validateConfig(config)) {
            form.addError(new FormMessage(null, Messages.TURNSTILE_NOT_CONFIGURED));
            return;
        }

        TurnstileHelper.addTurnstileToForm(form, config, context.getSession(),
                context.getUser(), TurnstileHelper.DEFAULT_ACTION_REGISTER);
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String captcha = formData.getFirst(TurnstileHelper.CF_TURNSTILE_RESPONSE);
        LOGGER.trace("Got Turnstile captcha: " + captcha);

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        if (!Validation.isBlank(captcha)) {
            String remoteAddr = context.getConnection().getRemoteAddr();
            if (TurnstileHelper.validateTurnstile(context.getSession(), captcha, config, remoteAddr)) {
                context.success();
                return;
            }
        }

        List<FormMessage> errors = new ArrayList<>();
        errors.add(new FormMessage(null, Messages.TURNSTILE_FAILED));
        context.error(Errors.INVALID_REGISTRATION);
        context.validationError(formData, errors);
        context.excludeOtherErrors();
    }

    @Override
    public void success(FormContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public FormAction create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
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
                .helpText("A meaningful name for this Turnstile context (e.g. login, register).")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(TurnstileHelper.DEFAULT_ACTION_REGISTER)
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
}
