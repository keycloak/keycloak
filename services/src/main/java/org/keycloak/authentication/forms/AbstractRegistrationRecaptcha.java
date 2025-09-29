/*
 *
 *  * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.authentication.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
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
import org.keycloak.utils.StringUtil;

public abstract class AbstractRegistrationRecaptcha implements FormAction, FormActionFactory {

    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";
    public static final String H_CAPTCHA_RESPONSE = "h-captcha-response";
    public static final Set<String> CAPTCHA_RESPONSE_FIELDS = Set.of(G_RECAPTCHA_RESPONSE, H_CAPTCHA_RESPONSE);
    public static final String RECAPTCHA_REFERENCE_CATEGORY = "recaptcha";

    // option keys
    public static final String SITE_KEY = "site.key";
    public static final String ACTION = "action";
    public static final String INVISIBLE = "recaptcha.v3";
    public static final String USE_RECAPTCHA_NET = "useRecaptchaNet";

    private static final Logger LOGGER = Logger.getLogger(AbstractRegistrationRecaptcha.class);

    @Override
    public String getReferenceCategory() {
        return RECAPTCHA_REFERENCE_CATEGORY;
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

    protected String getRecaptchaDomain(Map<String, String> config) {
        return Boolean.parseBoolean(config.get(USE_RECAPTCHA_NET)) ? "recaptcha.net" : "google.com";
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        LOGGER.trace("Building page with reCAPTCHA");

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        if (config == null) {
            form.addError(new FormMessage(null, Messages.RECAPTCHA_NOT_CONFIGURED));
            return;
        }

        if (!validateConfig(config)) {
            form.addError(new FormMessage(null, Messages.RECAPTCHA_NOT_CONFIGURED));
            return;
        }

        String userLanguageTag = context.getSession().getContext().resolveLocale(context.getUser())
                .toLanguageTag();
        boolean invisible = isInvisible(config);
        String action = resolveAction(config);

        form.setAttribute("recaptchaRequired", true);
        form.setAttribute("recaptchaSiteKey", config.get(SITE_KEY));
        form.setAttribute("recaptchaAction", action);
        form.setAttribute("recaptchaVisible", !invisible);
        form.setAttribute("recaptchaProviderId", getId());

        form.setAttribute("captchaRequired", true);
        form.setAttribute("captchaSiteKey", config.get(SITE_KEY));
        form.setAttribute("captchaAction", action);
        form.setAttribute("captchaVisible", !invisible);
        form.setAttribute("captchaProviderId", getId());

        form.addScript(getScriptUrl(config, userLanguageTag));
    }

    protected boolean isInvisible(Map<String, String> config) {
        return Boolean.parseBoolean(config.get(INVISIBLE));
    }

    protected String resolveAction(Map<String, String> config) {
        String action = config.get(ACTION);
        return StringUtil.isNullOrEmpty(action) ? "register" : action;
    }

    protected abstract String getScriptUrl(Map<String, String> config, String userLanguageTag);

    protected abstract boolean validateConfig(Map<String, String> config);

    protected abstract String getResponseFieldName();

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String captcha = formData.getFirst(getResponseFieldName());
        LOGGER.trace("Got captcha: " + captcha);

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        if (!Validation.isBlank(captcha)) {
            if (validate(context, captcha, config)) {
                context.success();
                return;
            }
        }

        List<FormMessage> errors = new ArrayList<>();
        errors.add(new FormMessage(null, Messages.RECAPTCHA_FAILED));
        context.error(Errors.INVALID_REGISTRATION);
        context.validationError(formData, errors);
        context.excludeOtherErrors();

    }

    protected abstract boolean validate(ValidationContext context, String captcha, Map<String, String> config);

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
                .name(ACTION)
                .label("Action Name")
                .helpText("A meaningful name for this reCAPTCHA context (e.g. login, register). "
                        + "An action name can only contain alphanumeric characters, "
                        + "slashes and underscores and is not case-sensitive.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("register")
                .add()
                .property()
                .name(USE_RECAPTCHA_NET)
                .label("Use recaptcha.net")
                .helpText("Whether to use recaptcha.net instead of google.com, "
                        + "which may have other cookies set.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .property()
                .name(INVISIBLE)
                .label("reCAPTCHA v3")
                .helpText("Whether the site key belongs to a v3 (invisible, score-based reCAPTCHA) "
                        + "or v2 site (visible, checkbox-based).")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .build();
    }
}
