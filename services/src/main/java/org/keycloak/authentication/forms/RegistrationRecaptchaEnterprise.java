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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.authentication.ValidationContext;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

public class RegistrationRecaptchaEnterprise extends AbstractRegistrationRecaptcha {
    public static final String PROVIDER_ID = "registration-recaptcha-enterprise";

    // option keys
    public static final String PROJECT_ID = "project.id";
    public static final String API_KEY = "api.key";
    public static final String SCORE_THRESHOLD = "score.threshold";

    private static final Logger LOGGER = Logger.getLogger(RegistrationRecaptchaEnterprise.class);

    @Override
    public String getDisplayType() {
        return "reCAPTCHA Enterprise";
    }

    @Override
    public String getHelpText() {
        return "Adds Google reCAPTCHA Enterprise to the form.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected boolean validateConfig(Map<String, String> config) {
        return !(Stream.of(PROJECT_ID, SITE_KEY, API_KEY, ACTION)
                .anyMatch(key -> StringUtil.isNullOrEmpty(config.get(key)))
                || parseDoubleFromConfig(config, SCORE_THRESHOLD) == null);
    }

    @Override
    protected String getScriptUrl(Map<String, String> config, String userLanguageTag) {
        return "https://www." + getRecaptchaDomain(config) + "/recaptcha/enterprise.js?hl=" + userLanguageTag;

    }

    @Override
    protected boolean validate(ValidationContext context, String captcha, Map<String, String> config) {
        LOGGER.trace("Requesting assessment of Google reCAPTCHA Enterprise");
        try {
            HttpPost request = buildAssessmentRequest(captcha, config);
            HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOGGER.errorf("Could not create reCAPTCHA assessment: %s", response.getStatusLine());
                EntityUtils.consumeQuietly(response.getEntity());
                throw new Exception(response.getStatusLine().getReasonPhrase());
            }

            RecaptchaAssessmentResponse assessment = JsonSerialization.readValue(
                    response.getEntity().getContent(), RecaptchaAssessmentResponse.class);
            LOGGER.tracef("Got assessment response: %s", assessment);

            String tokenAction = assessment.getTokenProperties().getAction();
            String expectedAction = assessment.getEvent().getExpectedAction();
            if (!tokenAction.equals(expectedAction)) {
                // This may indicates that an attacker is attempting to falsify actions
                LOGGER.warnf("The action name of the reCAPTCHA token '%s' does not match the expected action '%s'!",
                        tokenAction, expectedAction);
                return false;
            }

            boolean valid = assessment.getTokenProperties().isValid();
            double score = assessment.getRiskAnalysis().getScore();
            LOGGER.debugf("reCAPTCHA assessment: valid=%s, score=%f", valid, score);

            return valid && score >= parseDoubleFromConfig(config, SCORE_THRESHOLD);

        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }

        return false;
    }

    private HttpPost buildAssessmentRequest(String captcha, Map<String, String> config) throws IOException {

        String url = String.format("https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments?key=%s",
                config.get(PROJECT_ID), config.get(API_KEY));

        HttpPost request = new HttpPost(url);
        RecaptchaAssessmentRequest body = new RecaptchaAssessmentRequest(
                captcha, config.get(SITE_KEY), config.get(ACTION));
        request.setEntity(new StringEntity(JsonSerialization.writeValueAsString(body)));
        request.setHeader("Content-type", "application/json; charset=utf-8");

        LOGGER.tracef("Built assessment request: %s", body);
        return request;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = ProviderConfigurationBuilder.create()
                .property()
                .name(PROJECT_ID)
                .label("Project ID")
                .helpText("Project ID the site key belongs to.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(SITE_KEY)
                .label("reCAPTCHA Site Key")
                .helpText("The site key.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(API_KEY)
                .label("Google API Key")
                .helpText("An API key with the reCAPTCHA Enterprise API enabled in the given project ID.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .secret(true)
                .add()
                .property()
                .name(SCORE_THRESHOLD)
                .label("Min. Score Threshold")
                .helpText("The minimum score threshold for considering the reCAPTCHA valid (inclusive). "
                        + "Must be a valid double between 0.0 and 1.0.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("0.7")
                .add()
                .build();
        properties.addAll(super.getConfigProperties());
        return properties;
    }

    private Double parseDoubleFromConfig(Map<String, String> config, String key) {
        String value = config.getOrDefault(key, "");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.warnf("Could not parse config %s as double: '%s'", key, value);
        }
        return null;
    }
}
