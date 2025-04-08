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

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;

/**
 *
 * @author rmartinc
 */
public class PasskeysUsernamePasswordForm extends UsernamePasswordForm {

    private final WebAuthnConditionalUIAuthenticator webauthnAuth;

    public PasskeysUsernamePasswordForm(KeycloakSession session) {
        webauthnAuth = new WebAuthnConditionalUIAuthenticator(session, (context) -> createLoginForm(context.form()));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey(WebAuthnConstants.AUTHENTICATOR_DATA) || formData.containsKey(WebAuthnConstants.ERROR)) {
            // webauth form submission, try to action using the webauthn authenticator
            webauthnAuth.action(context);
        } else {
            // normal username and form authenticator
            super.action(context);
        }
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getUser() == null) {
            // setup webauthn data when the user is not already selected
            webauthnAuth.fillContextForm(context);
        }
        super.authenticate(context);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        if (context.getUser() == null) {
            // setup webauthn data when the user is not already selected
            webauthnAuth.fillContextForm(context);
        }
        return super.challenge(context, error, field);
    }
}
