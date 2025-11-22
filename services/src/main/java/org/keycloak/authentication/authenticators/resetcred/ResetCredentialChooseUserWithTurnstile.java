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

package org.keycloak.authentication.authenticators.resetcred;

import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.util.TurnstileHelper;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.services.validation.Validation;

/**
 * Reset Credential Choose User with Turnstile support
 */
public class ResetCredentialChooseUserWithTurnstile extends ResetCredentialChooseUser {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        addTurnstileToFormIfConfigured(context);
        super.authenticate(context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Validate Turnstile first if configured
        if (context.getAuthenticatorConfig() != null) {
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
            String captcha = formData.getFirst(TurnstileHelper.CF_TURNSTILE_RESPONSE);
            Map<String, String> config = context.getAuthenticatorConfig().getConfig();

            if (TurnstileHelper.validateConfig(config)) {
                if (Validation.isBlank(captcha) || !validateTurnstile(context, captcha, config)) {
                    context.getEvent().error(org.keycloak.events.Errors.INVALID_USER_CREDENTIALS);
                    Response challengeResponse = createChallengeWithError(context, "turnstileFailed");
                    context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
                    return;
                }
            }
        }

        // Continue with normal reset credential logic
        super.action(context);
    }

    protected void addTurnstileToFormIfConfigured(AuthenticationFlowContext context) {
        if (context.getAuthenticatorConfig() == null) {
            return;
        }

        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        if (!TurnstileHelper.validateConfig(config)) {
            return;
        }

        LoginFormsProvider form = context.form();
        if (form == null) {
            return;
        }

        TurnstileHelper.addTurnstileToForm(form, config, context.getSession(),
                context.getUser(), TurnstileHelper.DEFAULT_ACTION_RESET);
    }

    protected Response createChallengeWithError(AuthenticationFlowContext context, String error) {
        LoginFormsProvider form = context.form();
        if (error != null) {
            form.setError(error);
        }

        addTurnstileToFormIfConfigured(context);

        return form.createPasswordReset();
    }

    protected boolean validateTurnstile(AuthenticationFlowContext context, String captcha, Map<String, String> config) {
        String remoteAddr = context.getConnection().getRemoteAddr();
        return TurnstileHelper.validateTurnstile(context.getSession(), captcha, config, remoteAddr);
    }
}
