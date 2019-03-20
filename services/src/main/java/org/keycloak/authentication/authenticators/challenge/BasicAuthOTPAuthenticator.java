/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.authenticators.challenge;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BasicAuthOTPAuthenticator extends BasicAuthAuthenticator implements Authenticator {

    @Override
    protected boolean onAuthenticate(AuthenticationFlowContext context, String[] challenge) {
        String username = challenge[0];
        String password = challenge[1];
        OTPPolicy otpPolicy = context.getRealm().getOTPPolicy();
        int otpLength = otpPolicy.getDigits();

        if (password.length() < otpLength) {
            return false;
        }

        password = password.substring(0, password.length() - otpLength);

        if (checkUsernameAndPassword(context, username, password)) {
            String otp = challenge[1].substring(password.length(), challenge[1].length());

            if (checkOtp(context, otp)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkOtp(AuthenticationFlowContext context, String otp) {
        boolean valid = context.getSession().userCredentialManager().isValid(context.getRealm(), context.getUser(),
                UserCredentialModel.otp(context.getRealm().getOTPPolicy().getType(), otp));

        if (!valid) {
            context.getEvent().user(context.getUser()).error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = challenge(context, Messages.INVALID_TOTP);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            return false;
        }

        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, realm.getOTPPolicy().getType());
    }
}

