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

package org.keycloak.authentication.forms;

import java.util.List;
import java.util.Map;

import org.keycloak.authentication.ValidationContext;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ServicesLogger;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

public class RegistrationRecaptcha extends AbstractRegistrationRecaptcha {

    private static final Logger LOGGER = Logger.getLogger(RegistrationRecaptcha.class);
    public static final String PROVIDER_ID = "registration-recaptcha-action";

    // option keys
    public static final String SECRET_KEY = "secret.key";
    public static final String OLD_SECRET = "secret";

    @Override
    public String getDisplayType() {
        return "reCAPTCHA";
    }

    @Override
    public String getHelpText() {
        return "Adds Google reCAPTCHA to the form.";
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    protected boolean validateConfig(Map<String, String> config) {
        return !StringUtil.isNullOrEmpty(config.get(SITE_KEY)) &&
                (!StringUtil.isNullOrEmpty(config.get(SECRET_KEY)) || !StringUtil.isNullOrEmpty(config.get(OLD_SECRET)));
    }

    @Override
    protected boolean validate(ValidationContext context, String captcha, Map<String, String> config) {
        LOGGER.trace("Verifying reCAPTCHA using non-enterprise API");

        String url = "https://www." + getRecaptchaDomain(config) + "/recaptcha/api/siteverify";
        String secret = config.get(SECRET_KEY);
        if (StringUtil.isNullOrEmpty(secret)) {
            // migrate old config name to the new one
            secret = config.get(OLD_SECRET);
            if (!StringUtil.isNullOrEmpty(secret)) {
                config.put(SECRET_KEY, secret);
                config.remove(OLD_SECRET);
            }
        }

        try {
            var request = SimpleHttp.create(context.getSession()).doPost(url)
                    .param("secret", secret)
                    .param("response", captcha);
            if (context.getConnection().getRemoteAddr() != null) {
                request.param("remoteip", context.getConnection().getRemoteAddr());
            }
            Map json = request.asJson(Map.class);
            return Boolean.TRUE.equals(json.get("success"));
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }
        return false;
    }

    @Override
    protected String getScriptUrl(Map<String, String> config, String userLanguageTag) {
        return "https://www." + getRecaptchaDomain(config) + "/recaptcha/api.js?hl=" + userLanguageTag;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = ProviderConfigurationBuilder.create()
                .property()
                .name(SITE_KEY)
                .label("reCAPTCHA Site Key")
                .helpText("The site key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SECRET_KEY)
                .label("reCAPTCHA Secret")
                .helpText("The secret key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(true)
                .add()
                .build();
        properties.addAll(super.getConfigProperties());
        return properties;
    }
}
