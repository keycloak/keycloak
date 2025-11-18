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

package org.keycloak.authentication.authenticators.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

/**
 * Helper class for Cloudflare Turnstile CAPTCHA integration.
 * Provides common functionality for Turnstile validation and form integration.
 */
public class TurnstileHelper {

    private static final Logger LOGGER = Logger.getLogger(TurnstileHelper.class);

    // Form field name for Turnstile response token
    public static final String CF_TURNSTILE_RESPONSE = "cf-turnstile-response";

    // Configuration keys
    public static final String SITE_KEY = "site.key";
    public static final String SECRET_KEY = "secret.key";
    public static final String ACTION = "action";
    public static final String THEME = "theme";
    public static final String SIZE = "size";

    // Turnstile API endpoints
    public static final String TURNSTILE_SCRIPT_URL = "https://challenges.cloudflare.com/turnstile/v0/api.js";
    public static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    // Default configuration values
    public static final String DEFAULT_ACTION_REGISTER = "register";
    public static final String DEFAULT_ACTION_LOGIN = "login";
    public static final String DEFAULT_ACTION_RESET = "reset";
    public static final String DEFAULT_THEME = "auto";
    public static final String DEFAULT_SIZE = "normal";

    // Reference category for provider configuration
    public static final String TURNSTILE_REFERENCE_CATEGORY = "turnstile";

    private TurnstileHelper() {
        // Utility class, prevent instantiation
    }

    /**
     * Validates that required Turnstile configuration is present.
     *
     * @param config the configuration map
     * @return true if site key and secret key are configured
     */
    public static boolean validateConfig(Map<String, String> config) {
        return config != null &&
                !StringUtil.isNullOrEmpty(config.get(SITE_KEY)) &&
                !StringUtil.isNullOrEmpty(config.get(SECRET_KEY));
    }


    /**
     * Validates a Turnstile response token with Cloudflare's verification API.
     *
     * @param session the Keycloak session
     * @param captcha the Turnstile response token from the client
     * @param config the authenticator configuration
     * @param remoteAddr the client's remote IP address (optional)
     * @return true if validation succeeds
     */
    public static boolean validateTurnstile(KeycloakSession session, String captcha, Map<String, String> config, String remoteAddr) {
        LOGGER.trace("Verifying Turnstile using Cloudflare API");
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();

        HttpPost post = new HttpPost(TURNSTILE_VERIFY_URL);
        List<NameValuePair> formparams = new LinkedList<>();
        formparams.add(new BasicNameValuePair("secret", config.get(SECRET_KEY)));
        formparams.add(new BasicNameValuePair("response", captcha));
        if (remoteAddr != null) {
            formparams.add(new BasicNameValuePair("remoteip", remoteAddr));
        }

        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8);
            post.setEntity(form);
            
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                InputStream content = response.getEntity().getContent();
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = JsonSerialization.readValue(content, Map.class);
                    return Boolean.TRUE.equals(json.get("success"));
                } finally {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.turnstileFailed(e);
        }
        return false;
    }

    /**
     * Adds Turnstile widget attributes to a login form.
     *
     * @param form the login form provider
     * @param config the authenticator configuration
     * @param session the Keycloak session
     * @param user the user (may be null)
     * @param defaultAction the default action name if not configured
     */
    public static void addTurnstileToForm(LoginFormsProvider form, Map<String, String> config,
                                           KeycloakSession session, UserModel user, String defaultAction) {
        if (!validateConfig(config)) {
            return;
        }

        String userLanguageTag = session.getContext().resolveLocale(user).toLanguageTag();
        String action = StringUtil.isNullOrEmpty(config.get(ACTION)) ? defaultAction : config.get(ACTION);
        String theme = StringUtil.isNullOrEmpty(config.get(THEME)) ? DEFAULT_THEME : config.get(THEME);
        String size = StringUtil.isNullOrEmpty(config.get(SIZE)) ? DEFAULT_SIZE : config.get(SIZE);

        form.setAttribute("turnstileRequired", true);
        form.setAttribute("turnstileSiteKey", config.get(SITE_KEY));
        form.setAttribute("turnstileAction", action);
        form.setAttribute("turnstileTheme", theme);
        form.setAttribute("turnstileSize", size);
        form.setAttribute("turnstileLanguage", userLanguageTag);
        form.addScript(TURNSTILE_SCRIPT_URL);
    }
}
