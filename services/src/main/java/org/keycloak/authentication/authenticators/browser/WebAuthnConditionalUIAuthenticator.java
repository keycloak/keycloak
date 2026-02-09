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

import java.util.function.Function;

import jakarta.ws.rs.core.Response;

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.common.Profile;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;

/**
 *
 * @author rmartinc
 */
public class WebAuthnConditionalUIAuthenticator extends WebAuthnPasswordlessAuthenticator {

    private final Function<AuthenticationFlowContext, Response> errorChallenge;

    public WebAuthnConditionalUIAuthenticator(KeycloakSession session, Function<AuthenticationFlowContext, Response> errorChallenge) {
        super(session);
        this.errorChallenge = errorChallenge;
    }

    @Override
    public LoginFormsProvider fillContextForm(AuthenticationFlowContext context) {
        context.form().setAttribute(WebAuthnConstants.ENABLE_WEBAUTHN_CONDITIONAL_UI, Boolean.TRUE);
        return super.fillContextForm(context);
    }

    @Override
    protected Response createErrorResponse(AuthenticationFlowContext context, final String errorCase) {
        // the passkey failed, show error and maintain passkeys
        context.form().setError(errorCase, "");
        context.form().setAttribute(WebAuthnConstants.ENABLE_WEBAUTHN_CONDITIONAL_UI, Boolean.TRUE);

        AuthenticatorUtils.setupReauthenticationInUsernamePasswordFormError(context);

        fillContextForm(context);
        return errorChallenge.apply(context);
    }

    public boolean isPasskeysEnabled() {
        return isPasskeysEnabled(session);
    }

    static public boolean isPasskeysEnabled(KeycloakSession session) {
        return Profile.isFeatureEnabled(Profile.Feature.PASSKEYS) &&
                session.getContext().getRealm() != null &&
                Boolean.TRUE.equals(session.getContext().getRealm().getWebAuthnPolicyPasswordless().isPasskeysEnabled());
    }
}
