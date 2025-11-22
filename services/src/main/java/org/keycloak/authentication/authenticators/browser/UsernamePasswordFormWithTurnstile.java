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

import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.util.TurnstileHelper;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.validation.Validation;

/**
 * Username/Password form with Turnstile support
 */
public class UsernamePasswordFormWithTurnstile extends UsernamePasswordForm {

    public UsernamePasswordFormWithTurnstile(KeycloakSession session) {
        super(session);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        
        // Check if this is a WebAuthn/passkey submission first
        // Passkey submissions should bypass Turnstile validation since the user didn't interact with the form
        if (webauthnAuth != null && webauthnAuth.isPasskeysEnabled()
                && (formData.containsKey(WebAuthnConstants.AUTHENTICATOR_DATA) || formData.containsKey(WebAuthnConstants.ERROR))) {
            // webauth form submission, try to action using the webauthn authenticator
            webauthnAuth.action(context);
            return;
        }
        
        // Validate Turnstile for normal username/password submissions
        if (context.getAuthenticatorConfig() != null) {
            String captcha = formData.getFirst(TurnstileHelper.CF_TURNSTILE_RESPONSE);
            Map<String, String> config = context.getAuthenticatorConfig().getConfig();

            if (TurnstileHelper.validateConfig(config)) {
                if (Validation.isBlank(captcha) || !validateTurnstile(context, captcha, config)) {
                    context.getEvent().error(org.keycloak.events.Errors.INVALID_USER_CREDENTIALS);
                    Response challengeResponse = challenge(context, org.keycloak.services.messages.Messages.TURNSTILE_FAILED);
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                    return;
                }
            }
        }

        // Continue with normal username/password validation
        super.action(context);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, jakarta.ws.rs.core.MultivaluedMap<String, String> formData) {
        LoginFormsProvider form = context.form();
        // Add Turnstile to form if configured
        if (context.getAuthenticatorConfig() != null) {
            Map<String, String> config = context.getAuthenticatorConfig().getConfig();
            TurnstileHelper.addTurnstileToForm(form, config, context.getSession(),
                    context.getUser(), TurnstileHelper.DEFAULT_ACTION_LOGIN);
        }
        if (formData != null && !formData.isEmpty()) {
            form.setFormData(formData);
        }
        return form.createLoginUsernamePassword();
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        // Keep WebAuthn/passkeys behavior aligned with base class
        if (isConditionalPasskeysEnabled(context.getUser())) {
            webauthnAuth.fillContextForm(context);
        }

        LoginFormsProvider form = context.form()
                .setExecution(context.getExecution().getId());

        if (error != null) {
            if (field != null) {
                form.addError(new org.keycloak.models.utils.FormMessage(field, error));
            } else {
                form.setError(error);
            }
        }

        // Add Turnstile to form if configured
        if (context.getAuthenticatorConfig() != null) {
            Map<String, String> config = context.getAuthenticatorConfig().getConfig();
            TurnstileHelper.addTurnstileToForm(form, config, context.getSession(),
                    context.getUser(), TurnstileHelper.DEFAULT_ACTION_LOGIN);
        }

        return createLoginForm(form);
    }

    protected boolean validateTurnstile(AuthenticationFlowContext context, String captcha, Map<String, String> config) {
        String remoteAddr = context.getConnection().getRemoteAddr();
        return TurnstileHelper.validateTurnstile(context.getSession(), captcha, config, remoteAddr);
    }
}
